import { defineCollection, z } from "astro:content"
import { docsSchema } from "@astrojs/starlight/schema"

export const collections = {
    docs: defineCollection({ schema: docsSchema() }),
    changelogs: defineCollection({
        schema: z.object({
            version: z.object({
                code: z.number(),
                name: z.string(),
                title: z.string(),
            }),
            date: z.object({
                publish: z.date(),
            }),
            summary: z.string(),
        }),
    }),
}
