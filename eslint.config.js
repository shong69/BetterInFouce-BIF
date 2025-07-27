import js from "@eslint/js";
import globals from "globals";
import react from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import jsxA11y from "eslint-plugin-jsx-a11y";

export default [
  {
    ignores: ["dist", "node_modules", "*.min.js", "coverage"],
  },
  {
    files: ["**/*.{js,jsx}"],
    plugins: {
      react,
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
      "jsx-a11y": jsxA11y,
    },
    languageOptions: {
      ecmaVersion: "latest",
      globals: globals.browser,
      sourceType: "module",
      parserOptions: {
        ecmaFeatures: {
          jsx: true,
        },
      },
    },
    settings: {
      react: {
        version: "detect",
      },
    },
    rules: {
      ...js.configs.recommended.rules,
      "no-console": "warn",
      "no-debugger": "warn",
      "no-unused-vars": [
        "error",
        {
          varsIgnorePattern: "^_",
          argsIgnorePattern: "^_",
          ignoreRestSiblings: true,
        },
      ],
      "prefer-const": "error",
      "no-var": "error",
      "react/react-in-jsx-scope": "off",
      "react/jsx-uses-react": "off",
      "react/jsx-uses-vars": "error",
      "react/jsx-key": "error",
      "react/self-closing-comp": "warn",
      "react/jsx-pascal-case": "error",
      "react/jsx-no-target-blank": "error",
      "react/no-array-index-key": "warn",
      "react/no-unused-prop-types": "warn",
      "react/prop-types": "off",
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",
      "react-refresh/only-export-components": [
        "warn",
        { allowConstantExport: true },
      ],
      ...jsxA11y.configs.recommended.rules,
      "jsx-a11y/click-events-have-key-events": "warn",
      "jsx-a11y/no-static-element-interactions": "warn",
      curly: ["error", "all"],
      "no-eval": "error",
      "no-implied-eval": "error",
      "no-new-func": "error",
      quotes: [
        "warn",
        "double",
        {
          avoidEscape: true,
        },
      ],
      semi: ["warn", "always"],
    },
  },
  {
    files: [
      "*.config.js",
      "*.config.mjs",
      "eslint.config.js",
      "vite.config.js",
    ],

    languageOptions: {
      ecmaVersion: "latest",
      globals: globals.node,
      sourceType: "module",
    },
    rules: {
      "no-console": "off",
      "prefer-const": "warn",
    },
  },
];
