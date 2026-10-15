"use client";

import PaginaPrivada from "@/components/PaginaPrivada";
import { useAuth } from "@/hooks/useAuth";
import { ROTULO_PAPEL } from "@/types";

export default function Perfil() {
  return (
    <PaginaPrivada>
      <Conteudo />
    </PaginaPrivada>
  );
}

function Conteudo() {
  const { usuario } = useAuth();
  if (!usuario) return null;

  const dados: [string, string][] = [
    ["Nome", usuario.nome],
    ["Matrícula", usuario.matricula],
    ["E-mail", usuario.email],
    ["Telefone", usuario.telefone],
    ["Papel", ROTULO_PAPEL[usuario.role]],
  ];

  return (
    <>
      <h1 className="text-2xl font-bold text-marca-700">Meu perfil</h1>
      <dl className="mt-4 max-w-xl divide-y divide-gray-100 rounded-lg border border-gray-200 bg-white">
        {dados.map(([rotulo, valor]) => (
          <div key={rotulo} className="flex justify-between gap-4 px-4 py-3">
            <dt className="font-medium text-gray-700">{rotulo}</dt>
            <dd className="text-gray-900">{valor}</dd>
          </div>
        ))}
      </dl>
    </>
  );
}
