import { markdown } from "@astropub/md"
import type { APIRoute } from "astro"
import { getCollection, getEntry } from "astro:content"

export const GET: APIRoute = async ({ params }) => {
    const { versioncode } = params
    const changelog = await getEntry("changelogs", versioncode!)
    const rendered = await markdown(changelog?.body!)
    return new Response(JSON.stringify({ ...changelog, rendered }), {
        headers: {
            "content-type": "application/json; charset=utf-8",
        },
    })
}

export const getStaticPaths = async () => {
    const changelogs = await getCollection("changelogs")
    return changelogs.map((changelog) => ({
        params: {
            versioncode: changelog.data.version.code.toString(),
        },
    }))
}
