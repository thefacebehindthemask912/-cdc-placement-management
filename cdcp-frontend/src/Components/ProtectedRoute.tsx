import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRole: string;
}

const ProtectedRoute = ({ children, allowedRole }: ProtectedRouteProps) => {
  const { token, role } = useAuth();

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (role !== allowedRole) {
    // Redirect to their own dashboard
    return <Navigate to={`/${role}`} replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
