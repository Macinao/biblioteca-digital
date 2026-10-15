#!/usr/bin/env node
// Popula o SmartTeca com dados de demonstração, usando só a API pública do gateway
// (o mesmo caminho que o front usa), sem tocar direto em banco nem em filas.
//
//   node scripts/seed-demo.mjs                      cria os dados de demonstração
//   node scripts/seed-demo.mjs --reenviar-arquivos  reenvia capas e PDFs dos itens que já existem
//                                                   (use depois de configurar o Supabase Storage)
//
// Lê ADMIN_MATRICULA e ADMIN_SENHA do ambiente ou do arquivo .env na raiz do projeto.
// É idempotente: o que já existe é pulado, então pode rodar de novo sem duplicar nada.
// Requer Node 22+ (usa fetch, FormData e zlib.crc32 nativos: nenhuma dependência).

import { readFileSync } from "node:fs";
import { deflateSync, crc32 } from "node:zlib";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const RAIZ = join(dirname(fileURLToPath(import.meta.url)), "..");
const API = process.env.API_URL ?? "http://localhost:8080/api/v1";

// Contas de demonstração: senha única e pública de propósito (dado de teste, não segredo).
const SENHA_DEMO = "Demo12345";

function lerEnv() {
  try {
    const linhas = readFileSync(join(RAIZ, ".env"), "utf8").split(/\r?\n/);
    return Object.fromEntries(
      linhas.filter((l) => /^[A-Z_]+=/.test(l)).map((l) => [l.slice(0, l.indexOf("=")), l.slice(l.indexOf("=") + 1)]),
    );
  } catch {
    return {};
  }
}
const env = { ...lerEnv(), ...process.env };

// ---------- cliente HTTP com cookie de sessão ----------

let cookies = "";

async function chamar(metodo, caminho, corpo) {
  const cabecalhos = { Cookie: cookies };
  let body;
  if (corpo instanceof FormData) {
    body = corpo;
  } else if (corpo !== undefined) {
    cabecalhos["Content-Type"] = "application/json";
    body = JSON.stringify(corpo);
  }
  const resposta = await fetch(`${API}${caminho}`, { method: metodo, headers: cabecalhos, body });
  if (caminho === "/auth/login" && resposta.ok) {
    cookies = resposta.headers.getSetCookie().map((c) => c.split(";")[0]).join("; ");
  }
  const texto = await resposta.text();
  const dados = texto && resposta.headers.get("content-type")?.includes("json") ? JSON.parse(texto) : texto;
  return { status: resposta.status, dados };
}

async function exigir(metodo, caminho, corpo, aceitar = []) {
  const r = await chamar(metodo, caminho, corpo);
  if (r.status >= 400 && !aceitar.includes(r.status)) {
    throw new Error(`${metodo} ${caminho} -> ${r.status}: ${JSON.stringify(r.dados)}`);
  }
  return r;
}

// ---------- arquivos de exemplo gerados na hora ----------

function png(largura, altura, [r, g, b], faixa) {
  const linhas = [];
  for (let y = 0; y < altura; y++) {
    const linha = Buffer.alloc(1 + largura * 3); // 1 byte de filtro (0) + RGB por pixel
    for (let x = 0; x < largura; x++) {
      const destaque = y > altura * faixa[0] && y < altura * faixa[1]; // uma faixa clara dá cara de "capa"
      linha[1 + x * 3] = destaque ? 245 : r;
      linha[2 + x * 3] = destaque ? 192 : g;
      linha[3 + x * 3] = destaque ? 77 : b;
    }
    linhas.push(linha);
  }
  const bloco = (tipo, dados) => {
    const cabeca = Buffer.alloc(8);
    cabeca.writeUInt32BE(dados.length, 0);
    cabeca.write(tipo, 4, "ascii");
    const rodape = Buffer.alloc(4);
    rodape.writeUInt32BE(crc32(Buffer.concat([cabeca.subarray(4), dados])), 0);
    return Buffer.concat([cabeca, dados, rodape]);
  };
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(largura, 0);
  ihdr.writeUInt32BE(altura, 4);
  ihdr[8] = 8; // 8 bits por canal
  ihdr[9] = 2; // RGB
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    bloco("IHDR", ihdr),
    bloco("IDAT", deflateSync(Buffer.concat(linhas))),
    bloco("IEND", Buffer.alloc(0)),
  ]);
}

function pdf(titulo) {
  const texto = titulo.replace(/[^\x20-\x7e]/g, "?").replace(/[()\\]/g, "");
  const fluxo = `BT /F1 20 Tf 60 720 Td (${texto}) Tj ET`;
  const objetos = [
    "<< /Type /Catalog /Pages 2 0 R >>",
    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
    `<< /Length ${fluxo.length} >>\nstream\n${fluxo}\nendstream`,
    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
  ];
  let saida = "%PDF-1.4\n";
  const posicoes = [];
  objetos.forEach((o, i) => {
    posicoes.push(saida.length);
    saida += `${i + 1} 0 obj\n${o}\nendobj\n`;
  });
  const xref = saida.length;
  saida += `xref\n0 ${objetos.length + 1}\n0000000000 65535 f \n`;
  posicoes.forEach((p) => (saida += `${String(p).padStart(10, "0")} 00000 n \n`));
  saida += `trailer\n<< /Size ${objetos.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  return Buffer.from(saida, "latin1");
}

const CORES = [[21, 45, 93], [28, 59, 121], [90, 40, 80], [30, 90, 70], [120, 50, 40], [60, 60, 100]];

function capa(i) {
  return new Blob([png(200, 300, CORES[i % CORES.length], [0.55 + (i % 3) * 0.1, 0.62 + (i % 3) * 0.1])], { type: "image/png" });
}

function multipart(parte, dados, arquivos) {
  const form = new FormData();
  form.append(parte, new Blob([JSON.stringify(dados)], { type: "application/json" }));
  for (const [nome, blob, arquivo] of arquivos) form.append(nome, blob, arquivo);
  return form;
}

const pdfDe = (titulo) => new Blob([pdf(titulo)], { type: "application/pdf" });

// ---------- dados ----------

const CURSOS = ["Ciência da Computação", "Engenharia de Software", "Sistemas de Informação", "Administração", "Direito"];
const TEMAS = ["Algoritmos", "Banco de Dados", "Redes", "Inteligência Artificial", "Engenharia de Software", "Programação", "Gestão", "Literatura"];

const USUARIOS = [
  { nome: "Maria Bibliotecaria", matricula: "BIB00001", email: "bib00001@smartteca.local", telefone: "83999990001", role: "BIBLIOTECARIO" },
  { nome: "Paulo Professor", matricula: "PROF0001", email: "prof0001@smartteca.local", telefone: "83999990002", role: "PROFESSOR" },
  { nome: "Ana Aluna", matricula: "ALUNO0001", email: "aluno0001@smartteca.local", telefone: "83999990003", role: "ALUNO" },
  { nome: "Bruno Aluno", matricula: "ALUNO0002", email: "aluno0002@smartteca.local", telefone: "83999990004", role: "ALUNO" },
  { nome: "Carla Aluna", matricula: "ALUNO0003", email: "aluno0003@smartteca.local", telefone: "83999990005", role: "ALUNO" },
];

// [titulo, autor, descricao, ano, quantidade, cursos[], temas[]]
const LIVROS = [
  ["Código Limpo", "Robert C. Martin", "Habilidades práticas do Agile Software: nomes, funções, comentários e testes que deixam o código legível.", 2008, 4, [0, 1, 2], [5, 4]],
  ["Algoritmos: Teoria e Prática", "Thomas H. Cormen", "Referência clássica sobre projeto e análise de algoritmos, de ordenação a grafos.", 2012, 3, [0, 1], [0, 5]],
  ["Sistemas de Banco de Dados", "Ramez Elmasri e Shamkant Navathe", "Modelagem, SQL, normalização, transações e arquitetura de bancos relacionais.", 2011, 3, [0, 2], [1]],
  ["Redes de Computadores", "Andrew S. Tanenbaum", "Camadas, protocolos e o funcionamento da Internet, do meio físico à aplicação.", 2011, 2, [0, 2], [2]],
  ["Engenharia de Software", "Ian Sommerville", "Processos, requisitos, arquitetura, testes e evolução de sistemas de software.", 2011, 5, [1, 2], [4]],
  ["Inteligência Artificial", "Stuart Russell e Peter Norvig", "Agentes, busca, lógica, aprendizado de máquina e raciocínio sob incerteza.", 2013, 2, [0, 1], [3]],
  ["Padrões de Projeto", "Erich Gamma et al.", "Vinte e três padrões de projeto orientado a objetos reutilizáveis.", 2000, 3, [0, 1], [4, 5]],
  ["Refatoração", "Martin Fowler", "Como melhorar a estrutura de um código existente sem alterar seu comportamento.", 2004, 2, [1], [4, 5]],
  ["Administração: Teoria e Prática", "Idalberto Chiavenato", "Fundamentos da gestão: planejamento, organização, direção e controle.", 2014, 4, [3], [6]],
  ["Gestão de Projetos", "Vicente Falconi", "Métodos para planejar e entregar projetos no prazo e dentro do orçamento.", 2013, 3, [3, 2], [6]],
  ["Introdução ao Direito Digital", "Patrícia Peck Pinheiro", "Marco civil, LGPD, contratos eletrônicos e responsabilidade na rede.", 2016, 2, [4], [6]],
  ["Estruturas de Dados em Java", "Michael T. Goodrich", "Listas, pilhas, filas, árvores e tabelas hash implementadas em Java.", 2013, 0, [0, 1], [0, 5]],
];

const LIVROS_PUBLICOS = [
  ["Dom Casmurro", "Machado de Assis", "Bentinho narra sua vida e a dúvida sobre Capitu, em um dos clássicos da literatura brasileira.", 1899],
  ["Memórias Póstumas de Brás Cubas", "Machado de Assis", "O defunto autor conta sua história com ironia e humor ácido.", 1881],
  ["O Cortiço", "Aluísio Azevedo", "Romance naturalista sobre a vida em uma habitação coletiva no Rio de Janeiro.", 1890],
  ["Iracema", "José de Alencar", "Lenda do Ceará: o encontro entre a índia Iracema e o colonizador Martim.", 1865],
  ["O Guarani", "José de Alencar", "Romance indianista sobre Peri e Cecília no Brasil colonial.", 1857],
  ["A Moreninha", "Joaquim Manuel de Macedo", "Primeiro romance romântico brasileiro, passado em uma ilha do Rio de Janeiro.", 1844],
];

const TCCS = [
  ["Arquitetura de Microsserviços Orientada a Eventos", "Lucas Ferreira", "Estudo comparativo entre Kafka e RabbitMQ em sistemas acadêmicos.", 2025, 1, [4, 5]],
  ["Detecção de Fraudes com Aprendizado de Máquina", "Juliana Costa", "Modelos supervisionados aplicados a transações financeiras simuladas.", 2024, 0, [3]],
  ["Gestão do Conhecimento em Bibliotecas Universitárias", "Rafael Lima", "Impacto de sistemas de informação no uso do acervo.", 2023, 2, [6]],
  ["Otimização de Consultas em Bancos Relacionais", "Camila Souza", "Índices, planos de execução e desempenho em PostgreSQL.", 2024, 0, [1]],
  ["Proteção de Dados Pessoais em Sistemas Web", "Marcos Oliveira", "Adequação à LGPD em aplicações de pequeno porte.", 2025, 4, [6]],
  ["Redes Definidas por Software", "Fernanda Alves", "Controle centralizado de redes com OpenFlow.", 2022, 1, [2]],
];

const MATERIAIS = [
  ["Apostila de Algoritmos", "Prof. Paulo Professor", "Exercícios resolvidos de ordenação, busca e recursão.", 2025, [0, 1], [0, 5]],
  ["Introdução a SQL", "Prof. Paulo Professor", "Do SELECT às junções, com exemplos práticos.", 2025, [0, 2], [1]],
  ["Slides de Engenharia de Requisitos", "Prof. Paulo Professor", "Elicitação, especificação e validação de requisitos.", 2024, [1, 2], [4]],
  ["Guia de Git e GitHub", "Prof. Paulo Professor", "Branches, pull requests e fluxo de trabalho em equipe.", 2025, [0, 1, 2], [5, 4]],
  ["Lista de Exercícios de Redes", "Prof. Paulo Professor", "Endereçamento IP, sub-redes e roteamento.", 2024, [0, 2], [2]],
  ["Resumo de Gestão de Projetos", "Prof. Paulo Professor", "Ciclo de vida, escopo, prazo e custo.", 2024, [3, 2], [6]],
];

const dataApi = (ano) => `${ano}-03-15T00:00:00`;

// ---------- reenvio de arquivos ----------

const ids = (lista) => (lista ?? []).map((x) => x.id);

/** Todos os itens de um tipo, lendo as páginas da API (o servidor limita cada página a 100). */
async function todosOsItens(caminho) {
  const itens = [];
  for (let pagina = 0; ; pagina++) {
    const r = await exigir("GET", `${caminho}/listar?pagina=${pagina}&tamanho=100`);
    itens.push(...r.dados);
    if (r.dados.length < 100) return itens;
  }
}

/**
 * As capas/PDFs enviados quando o Supabase ainda não estava configurado foram para a fila morta do RabbitMQ.
 * Em vez de mexer na fila, atualiza cada item com os mesmos dados + os arquivos: o core publica de novo os comandos.
 */
async function reenviarArquivos() {
  const dadosDe = (item, extras) => ({
    id: item.id, titulo: item.titulo, autor: item.autor, descricao: item.descricao,
    dataPublicacao: item.dataPublicacao, temas: ids(item.temas), ...extras,
  });
  let enviados = 0;

  const itensLivro = new Map((await todosOsItens("/livro")).map((i) => [i.titulo, i]));
  for (const [i, [titulo]] of LIVROS.entries()) {
    const item = itensLivro.get(titulo);
    if (!item) continue;
    await exigir("PUT", "/livro/atualizar", multipart("livro", dadosDe(item, { cursos: ids(item.cursos), quantidade: item.quantidade }), [["capa", capa(i), `capa-${i}.png`]]));
    enviados++;
  }

  const itensPublicos = new Map((await todosOsItens("/livro-publico")).map((i) => [i.titulo, i]));
  for (const [i, [titulo]] of LIVROS_PUBLICOS.entries()) {
    const item = itensPublicos.get(titulo);
    if (!item) continue;
    await exigir("PUT", "/livro-publico/atualizar", multipart("livro-publico", dadosDe(item, { cursos: ids(item.cursos) }), [["conteudo", pdfDe(titulo), "livro.pdf"], ["capa", capa(i + 3), `capa-${i}.png`]]));
    enviados++;
  }

  const itensTcc = new Map((await todosOsItens("/tcc")).map((i) => [i.titulo, i]));
  for (const [titulo] of TCCS) {
    const item = itensTcc.get(titulo);
    if (!item) continue;
    await exigir("PUT", "/tcc/atualizar", multipart("tcc", dadosDe(item, { curso: item.curso?.id }), [["conteudo", pdfDe(titulo), "tcc.pdf"]]));
    enviados++;
  }

  const itensMateriais = new Map((await todosOsItens("/material-estudo")).map((i) => [i.titulo, i]));
  for (const [titulo] of MATERIAIS) {
    const item = itensMateriais.get(titulo);
    if (!item) continue;
    await exigir("PUT", "/material-estudo/atualizar", multipart("material-estudo", dadosDe(item, { cursos: ids(item.cursos) }), [["conteudo", pdfDe(titulo), "material.pdf"]]));
    enviados++;
  }

  console.log(`Arquivos reenviados para ${enviados} itens. O storage-service processa em segundo plano.`);
}

// ---------- execução ----------

async function main() {
  const matricula = env.ADMIN_MATRICULA;
  const senha = env.ADMIN_SENHA;
  if (!matricula || !senha) {
    throw new Error("Defina ADMIN_MATRICULA e ADMIN_SENHA (ambiente ou .env) e suba o core-service com elas.");
  }

  const login = await chamar("POST", "/auth/login", { matricula, senha });
  if (login.status !== 200) throw new Error(`Login do administrador falhou (${login.status}). O core-service subiu com essas variáveis?`);
  console.log("Administrador autenticado.");

  if (process.argv.includes("--reenviar-arquivos")) {
    await reenviarArquivos();
    return;
  }

  // --- usuários (422 = já existe) ---
  for (const u of USUARIOS) {
    const r = await exigir("POST", "/usuario", { ...u, senha: SENHA_DEMO }, [422]);
    console.log(`Usuário ${u.matricula}: ${r.status === 201 ? "criado" : "já existia"}`);
  }

  // --- cursos e temas: reaproveita os existentes (ids vêm do search-service) ---
  const existentes = async (caminho) => (await exigir("GET", caminho)).dados;
  const garantir = async (rotaCriar, rotaListar, nomes) => {
    const atuais = new Map((await existentes(rotaListar)).map((x) => [x.nome, x.id]));
    const ids = [];
    for (const nome of nomes) {
      if (atuais.has(nome)) {
        ids.push(atuais.get(nome));
      } else {
        ids.push((await exigir("POST", rotaCriar, { nome })).dados.id);
      }
    }
    return ids;
  };
  const cursoIds = await garantir("/curso/cadastrar", "/curso/listar", CURSOS);
  const temaIds = await garantir("/tema/cadastrar", "/tema/listar", TEMAS);
  console.log(`Cursos: ${cursoIds.length}, temas: ${temaIds.length}`);

  // --- materiais: pula o que já existe pelo título ---
  const titulos = async (caminho) => new Set((await existentes(caminho)).map((m) => m.titulo));
  const pick = (ids, indices) => indices.map((i) => ids[i]);

  const livrosExistentes = await titulos("/livro/listar");
  const livrosCriados = [];
  for (const [i, [titulo, autor, descricao, ano, quantidade, cursos, temas]] of LIVROS.entries()) {
    if (livrosExistentes.has(titulo)) continue;
    const dados = { titulo, autor, descricao, dataPublicacao: dataApi(ano), quantidade, cursos: pick(cursoIds, cursos), temas: pick(temaIds, temas) };
    const r = await exigir("POST", "/livro/cadastrar", multipart("livro", dados, [["capa", capa(i), `capa-${i}.png`]]));
    livrosCriados.push(r.dados);
  }
  console.log(`Livros novos: ${livrosCriados.length}`);

  const publicosExistentes = await titulos("/livro-publico/listar");
  let novos = 0;
  for (const [i, [titulo, autor, descricao, ano]] of LIVROS_PUBLICOS.entries()) {
    if (publicosExistentes.has(titulo)) continue;
    const dados = { titulo, autor, descricao, dataPublicacao: dataApi(ano), cursos: pick(cursoIds, [3, 4]), temas: pick(temaIds, [7]) };
    await exigir("POST", "/livro-publico/cadastrar", multipart("livro-publico", dados, [["conteudo", pdfDe(titulo), "livro.pdf"], ["capa", capa(i + 3), `capa-${i}.png`]]));
    novos++;
  }
  console.log(`Livros públicos novos: ${novos}`);

  const tccsExistentes = await titulos("/tcc/listar");
  novos = 0;
  for (const [titulo, autor, descricao, ano, curso, temas] of TCCS) {
    if (tccsExistentes.has(titulo)) continue;
    const dados = { titulo, autor, descricao, dataPublicacao: dataApi(ano), curso: cursoIds[curso], temas: pick(temaIds, temas) };
    await exigir("POST", "/tcc/cadastrar", multipart("tcc", dados, [["conteudo", pdfDe(titulo), "tcc.pdf"]]));
    novos++;
  }
  console.log(`TCCs novos: ${novos}`);

  const materiaisExistentes = await titulos("/material-estudo/listar");
  novos = 0;
  for (const [titulo, autor, descricao, ano, cursos, temas] of MATERIAIS) {
    if (materiaisExistentes.has(titulo)) continue;
    const dados = { titulo, autor, descricao, dataPublicacao: dataApi(ano), cursos: pick(cursoIds, cursos), temas: pick(temaIds, temas) };
    await exigir("POST", "/material-estudo/cadastrar", multipart("material-estudo", dados, [["conteudo", pdfDe(titulo), "material.pdf"]]));
    novos++;
  }
  console.log(`Materiais de estudo novos: ${novos}`);

  // --- reservas de exemplo (só na primeira carga, quando os livros acabaram de ser criados) ---
  if (livrosCriados.length > 0) {
    const reservas = [["ALUNO0001", 0], ["ALUNO0001", 4], ["ALUNO0002", 1]];
    for (const [aluno, indice] of reservas) {
      await exigir("POST", "/reserva/reservar", { usuario: aluno, livro: livrosCriados[indice].id });
    }
    console.log(`Reservas de exemplo: ${reservas.length}`);
  }

  console.log("\nPronto. Contas de demonstração (senha " + SENHA_DEMO + "): BIB00001, PROF0001, ALUNO0001..ALUNO0003.");
  console.log("Os dados chegam ao search-service por eventos: aguarde alguns segundos antes de listar.");
}

main().catch((e) => {
  console.error("Falhou:", e.message);
  process.exit(1);
});
