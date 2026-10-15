"use client";

interface Opcao {
  id: string;
  nome: string;
}

interface Props {
  rotulo: string;
  opcoes: Opcao[];
  valor: string[];
  aoMudar: (ids: string[]) => void;
  erro?: string;
}

/** Grupo de caixas de marcação controlado: devolve sempre uma lista de ids, mesmo com uma única opção. */
export default function SelecaoMultipla({ rotulo, opcoes, valor, aoMudar, erro }: Props) {
  function alternar(id: string) {
    aoMudar(valor.includes(id) ? valor.filter((v) => v !== id) : [...valor, id]);
  }

  return (
    <fieldset aria-invalid={Boolean(erro)}>
      <legend className="mb-1 text-sm font-medium text-gray-700">{rotulo}</legend>
      <div className="flex flex-wrap gap-x-4 gap-y-1 rounded border border-gray-300 bg-white p-2">
        {opcoes.length === 0 && <span className="text-sm text-gray-500">Nenhuma opção cadastrada.</span>}
        {opcoes.map((opcao) => (
          <label key={opcao.id} className="flex items-center gap-1.5 text-sm">
            <input type="checkbox" checked={valor.includes(opcao.id)} onChange={() => alternar(opcao.id)} />
            {opcao.nome}
          </label>
        ))}
      </div>
      {erro && <p className="mt-1 text-sm text-red-700">{erro}</p>}
    </fieldset>
  );
}
