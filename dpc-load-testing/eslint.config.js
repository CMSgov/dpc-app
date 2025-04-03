import { defineConfig, globalIgnores } from "eslint/config";
import js from "@eslint/js";

export default defineConfig([
    [ globalIgnores(["__tests__", "macaroonTests.js"]) ],
	{
		files: ["**/*.js"],
		plugins: { js },
		extends: ["js/recommended"],
		rules: {
		    "no-undef": "warn",
		    "no-unused-vars": "warn",
		},
		languageOptions: {
		    globals: {
		        __ENV: "readonly",
		    },
		},
	},
]);
