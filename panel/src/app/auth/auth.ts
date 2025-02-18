"use client"

export function getRedirectUrl(): string {
	// TODO properly handle prerendering here
	return typeof window !== 'undefined' ? `${window.location.origin}/auth/callback` : ""
} 
