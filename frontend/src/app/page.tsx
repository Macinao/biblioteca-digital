"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import Logo from "@/components/Logo";
import { useAuth } from "@/hooks/useAuth";
import { mensagemDeErro } from "@/lib/api";

interface Campos {
  matricula: string;
  senha: string;
}

export default function Login() {
  const { usuario, carregando, entrar } = useAuth();
  const router = useRouter();
  const [erro, setErro] = useState("");
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Campos>();

  // Quem já tem sessão não precisa ver o formulário.
  useEffect(() => {
    if (!carregando && usuario) router.replace("/home");
  }, [carregando, usuario, router]);

  async function aoEnviar(campos: Campos) {
    setErro("");
    try {
      await entrar(campos);
      router.replace("/home");
    } catch (e) {
      setErro(mensagemDeErro(e, "Não foi possível entrar."));
    }
  }

  return (
    <main className="grid min-h-screen place-items-center bg-marca-50 px-4">
      <form
        onSubmit={handleSubmit(aoEnviar)}
        noValidate
        className="w-full max-w-sm space-y-4 rounded-lg bg-white p-6 shadow"
        aria-labelledby="titulo-login"
      >
        <div className="text-marca-700">
          <Logo className="h-9 text-2xl" />
        </div>
        <h1 id="titulo-login" className="text-lg font-semibold text-gray-800">
          Entrar na biblioteca
        </h1>

        <div>
          <label htmlFor="matricula" className="mb-1 block text-sm font-medium text-gray-700">
            Matrícula
          </label>
          <input
            id="matricula"
            autoComplete="username"
            aria-invalid={Boolean(errors.matricula)}
            className="w-full rounded border border-gray-300 px-3 py-2"
            {...register("matricula", { required: "Informe sua matrícula." })}
          />
          {errors.matricula && <p className="mt-1 text-sm text-red-700">{errors.matricula.message}</p>}
        </div>

        <div>
          <label htmlFor="senha" className="mb-1 block text-sm font-medium text-gray-700">
            Senha
          </label>
          <input
            id="senha"
            type="password"
            autoComplete="current-password"
            aria-invalid={Boolean(errors.senha)}
            className="w-full rounded border border-gray-300 px-3 py-2"
            {...register("senha", { required: "Informe sua senha." })}
          />
          {errors.senha && <p className="mt-1 text-sm text-red-700">{errors.senha.message}</p>}
        </div>

        {erro && (
          <p role="alert" className="rounded bg-red-50 p-2 text-sm text-red-800">
            {erro}
          </p>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded bg-marca-700 py-2 font-semibold text-white hover:bg-marca-900 disabled:opacity-60"
        >
          {isSubmitting ? "Entrando…" : "Entrar"}
        </button>

        <p className="text-center text-xs text-gray-500">
          Não tem acesso? Procure o bibliotecário para criar sua conta.
        </p>
      </form>
    </main>
  );
}
