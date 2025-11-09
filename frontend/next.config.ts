import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "http",
        hostname: "localhost",
        port: "8080",
      },
    ],
  },
// 프록시 설정: 3000의 /api/* → 8080으로 전달
async rewrites() {
  return [
    {
      source: '/api/:path*',
      destination: 'http://localhost:8080/:path*', // 백엔드로 프록시
    },
  ];
},
};

export default nextConfig;
