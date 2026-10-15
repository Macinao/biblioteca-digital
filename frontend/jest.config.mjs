import nextJest from "next/jest.js";

// next/jest já configura SWC, CSS/imagens e variáveis de ambiente para o Jest.
const createJestConfig = nextJest({ dir: "./" });

export default createJestConfig({
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/jest.setup.ts"],
  moduleNameMapper: { "^@/(.*)$": "<rootDir>/src/$1" },
});
