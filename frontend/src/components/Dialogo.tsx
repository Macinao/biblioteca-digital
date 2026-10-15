"use client";

import { useEffect, useId, useRef } from "react";

interface Props {
  titulo: string;
  aoFechar: () => void;
  children: React.ReactNode;
}

/**
 * Usa o <dialog> nativo: o navegador já cuida do foco preso dentro dele, da tecla Esc e do fundo bloqueado.
 * Monte o componente só quando o diálogo deve estar aberto.
 */
export default function Dialogo({ titulo, aoFechar, children }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const idTitulo = useId();

  useEffect(() => {
    const dialogo = ref.current;
    if (dialogo && !dialogo.open) dialogo.showModal();
  }, []);

  return (
    <dialog
      ref={ref}
      onClose={aoFechar}
      aria-labelledby={idTitulo}
      className="m-auto max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-lg p-0 shadow-xl backdrop:bg-black/50"
    >
      <div className="p-5">
        <div className="mb-4 flex items-start justify-between gap-4">
          <h2 id={idTitulo} className="text-xl font-bold text-marca-700">
            {titulo}
          </h2>
          <button type="button" onClick={aoFechar} aria-label="Fechar" className="rounded px-2 text-2xl leading-none text-gray-500 hover:bg-gray-100">
            ×
          </button>
        </div>
        {children}
      </div>
    </dialog>
  );
}
