/** The header of a screen, with its title. Its actions join it when a screen first has some. */
export function PageHeader({ title }: { title: string }) {
  return (
    <header className="mb-6">
      <h1 className="text-h1 font-semibold">{title}</h1>
    </header>
  );
}
