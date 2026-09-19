export async function GET() {
  try {
    const response = await fetch(`${process.env.BACKEND_URL ?? "http://127.0.0.1:8080"}/actuator/health`, {cache:"no-store", signal:AbortSignal.timeout(5000)});
    if (response.ok) return Response.json({status:"UP"});
  } catch { /* Expose only availability, never internal errors. */ }
  return Response.json({status:"DOWN"}, {status:503});
}
