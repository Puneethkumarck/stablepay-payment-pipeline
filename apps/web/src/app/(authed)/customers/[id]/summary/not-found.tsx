import { NotFoundCard } from '~/components/not-found-card';

export default function NotFound() {
  return (
    <div className="page flex items-center justify-center pt-20">
      <NotFoundCard
        title="We can't find that customer"
        body="The customer may not exist, or you may not have access."
        primaryCta={{ label: 'Back to dashboard', href: '/' }}
      />
    </div>
  );
}
