import { defineConfig } from "astro/config"
import starlight from "@astrojs/starlight"

// https://astro.build/config
export default defineConfig({
    integrations: [
        starlight({
            title: "Revolt on Android Technical Documentation",
            social: {
                github: "https://github.com/revoltchat/android",
            },
            sidebar: [
                {
                    label: "Contributing",
                    items: [
                        // Each item here is one entry in the navigation menu.
                        {
                            label: "Guidelines",
                            link: "/contributing/guidelines",
                        },
                        {
                            label: "Setup",
                            link: "/contributing/setup",
                        },
                    ],
                },
                /* {
                    label: "Reference",
                    autogenerate: { directory: "reference" },
                }, */
            ],
            customCss: ["./src/styles/custom.css"],
        }),
    ],
})
