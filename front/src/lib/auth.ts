const TOKEN_KEY = "omos_jwt_token";

export const auth = {
  getToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(TOKEN_KEY);
  },

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  },

  removeToken(): void {
    localStorage.removeItem(TOKEN_KEY);
  },

  isLoggedIn(): boolean {
    return !!this.getToken();
  },
};
