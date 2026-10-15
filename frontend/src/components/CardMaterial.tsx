import Link from "next/link";
import { formatarData } from "@/lib/format";
import { cursosDe, MATERIAIS, type TipoMaterial } from "@/services/materiais";
import type { Material } from "@/types";
import CapaMaterial from "./CapaMaterial";

export default function CardMaterial({ tipo, material }: { tipo: TipoMaterial; material: Material }) {
  const config = MATERIAIS[tipo];
  const cursos = cursosDe(material);
  const disponivel = (material.quantidade ?? 0) > 0;

  return (
    <li className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm transition hover:shadow">
      <Link href={`${config.rota}/${material.id}`} className="flex h-full flex-col">
        {config.temCapa && <CapaMaterial tipo={tipo} id={material.id} titulo={material.titulo} className="h-48 w-full" />}
        <div className="flex flex-1 flex-col gap-1 p-4">
          <h2 className="font-semibold leading-snug text-marca-700">{material.titulo}</h2>
          <p className="text-sm text-gray-600">{material.autor}</p>
          <p className="text-xs text-gray-500">{formatarData(material.dataPublicacao)}</p>

          {cursos.length > 0 && (
            <ul className="mt-1 flex flex-wrap gap-1" aria-label="Cursos">
              {cursos.map((curso) => (
                <li key={curso.id} className="rounded bg-marca-50 px-2 py-0.5 text-xs text-marca-700">
                  {curso.nome}
                </li>
              ))}
            </ul>
          )}

          {config.temQuantidade && (
            <p
              className={`mt-auto pt-2 text-sm font-medium ${disponivel ? "text-green-700" : "text-red-700"}`}
            >
              {disponivel ? `${material.quantidade} disponível(is)` : "Indisponível"}
            </p>
          )}
        </div>
      </Link>
    </li>
  );
}
