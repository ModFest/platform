import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
	title: "ModFest Panel",
	description: "Panel for the ModFest platform",
};

export default function RootLayout({
	children,
}: Readonly<{
	children: React.ReactNode;
}>) {
	return (
		<html lang="en">
			<body>
				{children}
			</body>
		</html>
	);
}
