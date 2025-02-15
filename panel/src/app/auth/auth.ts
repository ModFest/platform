"use client"

export function getRedirectUrl(): string {
	return `${window.location.origin}/auth/callback`
} 
