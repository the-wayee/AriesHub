const memberRoutes = [
  "/home",
  "/community",
  "/my-content",
  "/members",
  "/account",
  "/learn",
  "/checkout",
  "/admin",
  "/studio",
];
export function isProtectedRoute(path: string) {
  return memberRoutes.some(
    (route) => path === route || path.startsWith(`${route}/`),
  );
}
