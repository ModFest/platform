import type { NextConfig } from "next";
import { PHASE_DEVELOPMENT_SERVER } from "next/constants"
import fs from 'node:fs/promises';

const withBundleAnalyzer = require('@next/bundle-analyzer')({
	enabled: process.env.ANALYZE === 'true',
})

async function getDevUrl(): Promise<string> {
	const gradlePropsPath = __dirname+"/../gradle.properties"
	const gradleProps = await fs.readFile(gradlePropsPath, {encoding: "utf-8"})
	var lines = gradleProps.split("\n")
	lines = lines.map(l => l.trim())
	lines = lines.filter(l => l.length > 0 && !l.startsWith("#"))
	var properties: Record<string, string> = {}
	for (const line of lines) {
		properties[line.split("=")[0]] = line.split("=")[1]
	}
	return `http://${properties["dev.api.address"]}:${properties["dev.api.port"]}`
}

export default async function createConfig(phase: string): Promise<NextConfig> {
	var env = {}
	if (phase === PHASE_DEVELOPMENT_SERVER) {
		env = {
			"DEV_SERVER_URL": await getDevUrl(),
			...env
		}
	}

	return withBundleAnalyzer({
		output: "standalone",
		env: env,
		// TODO these shouldn't really be ignored
		eslint: {
			ignoreDuringBuilds: true,
		},
		typescript: {
			ignoreBuildErrors: true,
		},
	})
}
