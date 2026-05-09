import { fireEvent, render, screen } from '@testing-library/react';
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
    render(<DataTable columns={columns} rows={rows} />);
    expect(screen.getByText('ID')).toBeInTheDocument();
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
  });

  it('renders row data', () => {
    render(<DataTable columns={columns} rows={rows} />);
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
  });

  it('renders empty state when no rows', () => {
    render(<DataTable columns={columns} rows={[]} emptyMessage="Nothing here" />);
    expect(screen.getByTestId('empty')).toHaveTextContent('Nothing here');
  });

  it('calls onRowClick when row is clicked', () => {
    const onClick = vi.fn();
    render(<DataTable columns={columns} rows={rows} onRowClick={onClick} />);
    fireEvent.click(screen.getByText('Alice'));
    expect(onClick).toHaveBeenCalledWith(rows[0]);
  });

  it('supports keyboard navigation on rows', () => {
    const onClick = vi.fn();
    render(<DataTable columns={columns} rows={rows} onRowClick={onClick} />);
    const row = screen.getByText('Alice').closest('tr') as HTMLElement;
    fireEvent.keyDown(row, { key: 'Enter' });
    expect(onClick).toHaveBeenCalledWith(rows[0]);
  });

  it('renders skeleton rows when loading', () => {
    const { container } = render(<DataTable columns={columns} rows={[]} loading />);
    const skeletons = container.querySelectorAll('[data-slot="skeleton"]');
    expect(skeletons.length).toBe(9);
  });

  it('renders sentinel row when hasMore is true', () => {
    render(<DataTable columns={columns} rows={rows} hasMore onLoadMore={() => {}} />);
    expect(screen.getByTestId('data-table-sentinel')).toBeInTheDocument();
  });

  it('does not render sentinel when hasMore is false', () => {
    render(<DataTable columns={columns} rows={rows} />);
    expect(screen.queryByTestId('data-table-sentinel')).toBeNull();
  });

  it('supports custom column render', () => {
    const customColumns: ColumnConfig<TestRow>[] = [
      { key: 'name', label: 'Name', render: (v) => <strong>{String(v)}</strong> },
    ];
    render(<DataTable columns={customColumns} rows={rows} />);
    expect(screen.getByText('Alice').tagName).toBe('STRONG');
  });
});
