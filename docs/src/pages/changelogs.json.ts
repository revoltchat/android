import type { APIRoute } from "astro"
import { getCollection } from "astro:content"

export const GET: APIRoute = async () => {
    const changelogs = await getCollection("changelogs")
    return new Response(
        JSON.stringify({
            changelogs: changelogs.map((changelog) => ({
                ...changelog.data,
            })),
        }),
        {
            headers: {
                "content-type": "application/json; charset=utf-8",
            },
        }
    )
}

export const getStaticPaths = async () => {
    const changelogs = await getCollection("changelogs")
    return changelogs.map((changelog) => ({
        params: {
            versioncode: changelog.data.version.code.toString(),
        },
    }))
}
