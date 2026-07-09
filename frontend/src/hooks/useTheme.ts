import { useEffect, useState } from "react";

export type ThemeMode = "light" | "dark" | "system";

const STORAGE_KEY = "clipper-theme";

// Tema em 3 posições: claro/escuro forçados ou seguir o sistema.
// A mecânica (mesma do mockup): "system" = NENHUM data-theme no <html>,
// deixando o @media (prefers-color-scheme) do CSS decidir; claro/escuro
// gravam data-theme, que os blocos :root[data-theme=...] fazem vencer.
export function useTheme(): [ThemeMode, (mode: ThemeMode) => void] {
  const [mode, setMode] = useState<ThemeMode>(() => {
    // localStorage pode estar bloqueado (modo privado) — falha vira default.
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved === "light" || saved === "dark" || saved === "system") {
        return saved;
      }
    } catch {
      /* segue no default */
    }
    return "system";
  });

  useEffect(() => {
    const root = document.documentElement;
    if (mode === "system") {
      root.removeAttribute("data-theme");
    } else {
      root.setAttribute("data-theme", mode);
    }
    try {
      localStorage.setItem(STORAGE_KEY, mode);
    } catch {
      /* sem persistência, mas o tema da sessão funciona */
    }
  }, [mode]);

  return [mode, setMode];
}
