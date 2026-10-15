"use client";

import { useParams } from "next/navigation";
import DetalheMaterial from "@/components/DetalheMaterial";

export default function Pagina() {
  const { id } = useParams<{ id: string }>();
  return <DetalheMaterial tipo="livro" id={id} />;
}
