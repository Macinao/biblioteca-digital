"use client";

import { useState } from "react";
import { TAMANHO_PAGINA_RESERVAS, useDevolver, useTodasReservas } from "@/hooks/useReservas";
import { mensagemDeErro } from "@/lib/api";
import ListaReservas from "./ListaReservas";
import Paginador from "./Paginador";
import PaginaPrivada from "./PaginaPrivada";

export default function GerenciarReservas() {
  return (
    <PaginaPrivada papelMinimo="BIBLIOTECARIO">
      <Conteudo />
    </PaginaPrivada>
  );
}

function Conteudo() {
  const [pagina, setPagina] = useState(0);
  const { data, isPending, isError, error, isPlaceholderData } = useTodasReservas(pagina);
  const devolver = useDevolver();
  const [erro, setErro] = useState("");

  return (
    <>
      <h1 className="text-2xl font-bold text-marca-700">Reservas</h1>
      <p className="mt-1 text-sm text-gray-600">Empréstimos em aberto. Para criar uma reserva, abra o livro e informe a matrícula do aluno.</p>

      {isPending && <p role="status" className="mt-4">Carregando…</p>}
      {isError && <p role="alert" className="mt-4 rounded bg-red-50 p-3 text-red-800">{mensagemDeErro(error, "Não foi possível carregar as reservas.")}</p>}
      {erro && <p role="alert" className="mt-4 rounded bg-red-50 p-3 text-red-800">{erro}</p>}

      {data && (
        <ListaReservas
          reservas={data.itens}
          mostrarUsuario
          devolvendoId={devolver.isPending ? devolver.variables : undefined}
          aoDevolver={(reserva) => {
            setErro("");
            devolver.mutate(reserva.id, { onError: (e) => setErro(mensagemDeErro(e, "Não foi possível registrar a devolução.")) });
          }}
        />
      )}
      {data && <Paginador pagina={pagina} total={data.total} tamanho={TAMANHO_PAGINA_RESERVAS} aoMudar={setPagina} ocupado={isPlaceholderData} />}
    </>
  );
}
