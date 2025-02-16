import type { NextConfig } from "next";

const withBundleAnalyzer = require('@next/bundle-analyzer')({
	enabled: process.env.ANALYZE === 'true',
})

const nextConfig: NextConfig = withBundleAnalyzer({
  /* config options here */
});

export default nextConfig;
