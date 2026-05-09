import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { type ColumnConfig, DataTable } from './data-table';

interface TestRow {
  id: string;
  name: string;
  status: string;
  [key: string]: unknown;
}

const columns: ColumnConfig<TestRow>[] = [
  { key: 'id', label: 'ID', width: '100px' },
  { key: 'name', label: 'Name' },
  { key: 'status', label: 'Status' },
];

const rows: TestRow[] = [
  { id: '1', name: 'Alice', status: 'COMPLETED' },
  { id: '2', name: 'Bob', status: 'FAILED' },
];

describe('DataTable', () => {
  it('renders column headers', () => {
    // act
    render(<DataTable columns={columns} rows={rows} />);

    // assert
    expect(screen.getByText('ID')).toBeInTheDocument();
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
  });

  it('renders row data', () => {
    // act
    render(<DataTable columns={columns} rows={rows} />);

    // assert
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
  });

  it('renders empty state when no rows', () => {
    // act
    render(<DataTable columns={columns} rows={[]} emptyMessage="Nothing here" />);

    // assert
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
  });

  it('calls onRowClick when row is clicked', async () => {
    // arrange
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(<DataTable columns={columns} rows={rows} onRowClick={onClick} />);

    // act
    await user.click(screen.getByText('Alice'));

    // assert
    expect(onClick).toHaveBeenCalledWith(rows[0]);
  });

  it('supports keyboard navigation on rows', async () => {
    // arrange
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(<DataTable columns={columns} rows={rows} onRowClick={onClick} />);
    const row = screen.getByText('Alice').closest('tr') as HTMLElement;

    // act
    row.focus();
    await user.keyboard('{Enter}');

    // assert
    expect(onClick).toHaveBeenCalledWith(rows[0]);
  });

  it('renders skeleton rows when loading', () => {
    // act
    const { container } = render(<DataTable columns={columns} rows={[]} loading />);

    // assert
    const skeletons = container.querySelectorAll('[data-slot="skeleton"]');
    expect(skeletons.length).toBe(9);
  });

  it('renders sentinel row when hasMore is true', () => {
    // act
    render(<DataTable columns={columns} rows={rows} hasMore onLoadMore={() => {}} />);

    // assert
    expect(screen.getByTestId('data-table-sentinel')).toBeInTheDocument();
  });

  it('does not render sentinel when hasMore is false', () => {
    // act
    render(<DataTable columns={columns} rows={rows} />);

    // assert
    expect(screen.queryByTestId('data-table-sentinel')).toBeNull();
  });

  it('supports custom column render', () => {
    // arrange
    const customColumns: ColumnConfig<TestRow>[] = [
      { key: 'name', label: 'Name', render: (v) => <strong>{String(v)}</strong> },
    ];

    // act
    render(<DataTable columns={customColumns} rows={rows} />);

    // assert
    expect(screen.getByText('Alice').tagName).toBe('STRONG');
  });
});
