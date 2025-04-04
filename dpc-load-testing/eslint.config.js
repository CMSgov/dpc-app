import { defineConfig, globalIgnores } from "eslint/config";
import js from "@eslint/js";

export default defineConfig([
    [ globalIgnores(["__tests__", "macaroonTests.js"]) ],
	{
		files: ["**/*.js"],
		plugins: { js },
		extends: ["js/recommended"],
		languageOptions: {
		    globals: {
		        __ENV: "readonly",
		    },
		},
	},
]);
