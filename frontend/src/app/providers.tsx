"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import AssistenteGlobal from "@/components/AssistenteGlobal";
import { AuthProvider } from "@/hooks/useAuth";

export default function Providers({ children }: { children: React.ReactNode }) {
  // useState garante um QueryClient por navegador (e não um novo a cada render).
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60_000, // o acervo muda pouco: evita refazer a mesma busca a cada troca de página
            refetchOnWindowFocus: false,
            retry: 1,
          },
        },
      }),
  );
  return (
    <QueryClientProvider client={client}>
      <AuthProvider>
        {children}
        <AssistenteGlobal />
      </AuthProvider>
    </QueryClientProvider>
  );
}
