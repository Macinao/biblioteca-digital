"use client";

import { useState } from "react";
import { materiaisService, type TipoMaterial } from "@/services/materiais";

interface Props {
  tipo: TipoMaterial;
  id: string;
  titulo: string;
  className?: string;
}

/**
 * Capa vinda do storage-service. É um <img> comum (e não next/image) porque o arquivo exige o cookie de
 * sessão, que o otimizador de imagens do Next não teria. Se a capa não carregar (ainda não enviada, storage
 * fora do ar), mostra um bloco com a inicial do título em vez de uma imagem quebrada.
 */
export default function CapaMaterial({ tipo, id, titulo, className = "" }: Props) {
  const [falhou, setFalhou] = useState(false);

  if (falhou) {
    return (
      <div
        role="img"
        aria-label={`Sem capa: ${titulo}`}
        className={`grid place-items-center bg-marca-700 text-4xl font-bold text-destaque ${className}`}
      >
        {titulo.charAt(0).toUpperCase()}
      </div>
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={materiaisService.urlCapa(tipo, id)}
      alt={`Capa de ${titulo}`}
      loading="lazy"
      onError={() => setFalhou(true)}
      className={`object-cover ${className}`}
    />
  );
}
