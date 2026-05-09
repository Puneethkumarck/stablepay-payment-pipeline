'use client';

import { ErrorBoundaryCard } from '~/components/error-boundary-card';

export default function AuthedError({ reset }: { error: Error; reset: () => void }) {
  return (
    <div className="page flex items-center justify-center pt-20">
      <ErrorBoundaryCard onReset={reset} />
    </div>
  );
}
