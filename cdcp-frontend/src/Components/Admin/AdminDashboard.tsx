import { useEffect, useState } from 'react';
import Navbar from '../Navbar';
import API from '../../api/axios';

interface Stats {
  totalStudents: number;
  totalCompanies: number;
  totalJobs: number;
  totalApplications: number;
  totalPlacements: number;
}

interface UserEntry {
  id: number;
  email: string;
  role: string;
  firstName?: string;
  lastName?: string;
  department?: string;
  cgpa?: number;
}

const AdminDashboard = () => {
  const [stats, setStats] = useState<Stats | null>(null);
  const [users, setUsers] = useState<UserEntry[]>([]);
  const [companyStats, setCompanyStats] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'overview' | 'users' | 'reports'>('overview');
  const [message, setMessage] = useState('');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [statsRes, usersRes, compRes] = await Promise.all([
        API.get('/admin/stats'),
        API.get('/admin/users'),
        API.get('/admin/company-stats'),
      ]);
      setStats(statsRes.data);
      setUsers(usersRes.data || []);
      setCompanyStats(compRes.data || {});
    } catch {
      setMessage('Failed to load admin data.');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteUser = async (id: number, email: string) => {
    if (!window.confirm(`Delete user "${email}"? This action cannot be undone.`)) return;
    try {
      await API.delete(`/admin/users/${id}`);
      setUsers(prev => prev.filter(u => u.id !== id));
      setMessage(`User "${email}" deleted.`);
    } catch {
      setMessage('Failed to delete user.');
    }
  };

  const tabStyle = (tab: string): React.CSSProperties => ({
    padding: '8px 18px',
    cursor: 'pointer',
    border: 'none',
    borderBottom: activeTab === tab ? '3px solid #1a1a2e' : '3px solid transparent',
    background: 'none',
    fontWeight: activeTab === tab ? 'bold' : 'normal',
    fontSize: '0.95rem',
  });

  return (
    <>
      <Navbar />
      <div style={styles.container}>
        <h2>Admin Dashboard</h2>
        <p>Career Development & Placement Management System — Placement Officer Control Panel</p>

        {message && <div style={styles.info}>{message}</div>}

        <div style={styles.tabs}>
          <button style={tabStyle('overview')} onClick={() => setActiveTab('overview')}>Overview</button>
          <button style={tabStyle('users')} onClick={() => setActiveTab('users')}>User Management</button>
          <button style={tabStyle('reports')} onClick={() => setActiveTab('reports')}>Reports</button>
        </div>

        {loading ? <p>Loading...</p> : (
          <>
            {activeTab === 'overview' && stats && (
              <div style={styles.statsGrid}>
                {[
                  { label: 'Students Registered', value: stats.totalStudents },
                  { label: 'Companies', value: stats.totalCompanies },
                  { label: 'Jobs Posted', value: stats.totalJobs },
                  { label: 'Total Applications', value: stats.totalApplications },
                  { label: 'Students Placed', value: stats.totalPlacements },
                ].map(({ label, value }) => (
                  <div key={label} style={styles.statCard}>
                    <div style={styles.statNumber}>{value}</div>
                    <div style={styles.statLabel}>{label}</div>
                  </div>
                ))}
              </div>
            )}

            {activeTab === 'users' && (
              <>
                <h3>All Users ({users.length})</h3>
                <table style={styles.table}>
                  <thead>
                    <tr style={styles.theadRow}>
                      <th style={styles.th}>#</th>
                      <th style={styles.th}>Email / Username</th>
                      <th style={styles.th}>Role</th>
                      <th style={styles.th}>Name</th>
                      <th style={styles.th}>Department</th>
                      <th style={styles.th}>CGPA</th>
                      <th style={styles.th}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((u, idx) => (
                      <tr key={u.id} style={idx % 2 === 0 ? styles.evenRow : {}}>
                        <td style={styles.td}>{idx + 1}</td>
                        <td style={styles.td}>{u.email}</td>
                        <td style={styles.td}><span style={{ ...styles.roleBadge, backgroundColor: u.role === 'admin' ? '#ffe0b2' : u.role === 'company' ? '#c8e6c9' : '#bbdefb' }}>{u.role}</span></td>
                        <td style={styles.td}>{u.firstName || u.lastName ? `${u.firstName || ''} ${u.lastName || ''}`.trim() : '—'}</td>
                        <td style={styles.td}>{u.department || '—'}</td>
                        <td style={styles.td}>{u.cgpa ?? '—'}</td>
                        <td style={styles.td}>
                          {u.role !== 'admin' && (
                            <button style={styles.deleteBtn} onClick={() => handleDeleteUser(u.id, u.email)}>Delete</button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </>
            )}

            {activeTab === 'reports' && (
              <>
                <h3>Company-wise Applications</h3>
                {Object.keys(companyStats).length === 0 ? (
                  <p>No placement data available yet.</p>
                ) : (
                  <table style={styles.table}>
                    <thead>
                      <tr style={styles.theadRow}>
                        <th style={styles.th}>Company</th>
                        <th style={styles.th}>Total Applications Received</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(companyStats).map(([company, count], idx) => (
                        <tr key={company} style={idx % 2 === 0 ? styles.evenRow : {}}>
                          <td style={styles.td}>{company}</td>
                          <td style={styles.td}>{count}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </>
            )}
          </>
        )}
      </div>
    </>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: { padding: '24px' },
  info: { background: '#d4edda', color: '#155724', padding: '10px 14px', borderRadius: '4px', marginBottom: '12px' },
  tabs: { display: 'flex', borderBottom: '1px solid #ddd', marginBottom: '20px', marginTop: '16px' },
  statsGrid: { display: 'flex', gap: '16px', marginTop: '20px', flexWrap: 'wrap' },
  statCard: { background: '#fff', border: '1px solid #ddd', borderRadius: '8px', padding: '24px 32px', textAlign: 'center', minWidth: '150px' },
  statNumber: { fontSize: '2rem', fontWeight: 'bold', color: '#1a1a2e' },
  statLabel: { fontSize: '0.9rem', color: '#666', marginTop: '6px' },
  table: { width: '100%', borderCollapse: 'collapse', marginTop: '12px', background: '#fff' },
  theadRow: { backgroundColor: '#1a1a2e', color: '#fff' },
  th: { padding: '10px 14px', textAlign: 'left', fontWeight: 600 },
  td: { padding: '10px 14px', borderBottom: '1px solid #eee' },
  evenRow: { backgroundColor: '#f9f9f9' },
  roleBadge: { padding: '2px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 'bold' },
  deleteBtn: { padding: '5px 12px', backgroundColor: '#e74c3c', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' },
};

export default AdminDashboard;
