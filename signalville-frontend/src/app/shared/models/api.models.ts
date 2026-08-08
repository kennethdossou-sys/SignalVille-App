/**
 * Types miroir des schemas du contrat docs/api/signalville-openapi.yaml.
 * Toute divergence ici est un bug : le contrat fait foi.
 */

export type RoleName = 'CITOYEN' | 'AGENT' | 'SUPERVISEUR' | 'ADMINISTRATEUR';
export type AccountStatus = 'ACTIF' | 'SUSPENDU' | 'DESACTIVE';
export type Priority = 'BASSE' | 'MOYENNE' | 'HAUTE' | 'CRITIQUE';

export type ReportStatus =
  | 'NOUVEAU'
  | 'AFFECTE'
  | 'EN_COURS'
  | 'RESOLU'
  | 'CLOTURE'
  | 'REOUVERT'
  | 'REJETE'
  | 'ANNULE';

export interface UserResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: RoleName;
  status: AccountStatus;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
  termsAccepted: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface CategoryResponse {
  id: string;
  name: string;
  description?: string;
  icon?: string;
  defaultPriority: Priority;
  targetDelayHours: number;
  active: boolean;
  createdAt: string;
}

export interface PhotoResponse {
  id: string;
  url: string;
  description?: string;
  order: number;
  createdAt: string;
}

export interface HistoryResponse {
  id: string;
  previousStatus: ReportStatus | null;
  newStatus: ReportStatus;
  comment?: string;
  actor?: UserResponse;
  changedAt: string;
}

export interface ReportResponse {
  id: string;
  reference: string;
  title: string;
  description: string;
  status: ReportStatus;
  priority: Priority;
  category: CategoryResponse;
  latitude: number;
  longitude: number;
  address: string;
  district?: string;
  municipality?: string;
  photos: PhotoResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface ReportDetailResponse extends ReportResponse {
  citizen: UserResponse;
  activeIntervention: unknown | null;
  history: HistoryResponse[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}
