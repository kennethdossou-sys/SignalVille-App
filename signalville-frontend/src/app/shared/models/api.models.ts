export type Role = 'CITOYEN' | 'AGENT' | 'SUPERVISEUR' | 'ADMINISTRATEUR';

export type AccountStatus = 'ACTIF' | 'SUSPENDU' | 'DESACTIVE';

export type ReportStatus = 'NOUVEAU' | 'AFFECTE' | 'EN_COURS' | 'RESOLU' | 'REOUVERT' | 'CLOTURE' | 'REJETE' | 'ANNULE';

export type Priority = 'BASSE' | 'MOYENNE' | 'HAUTE' | 'CRITIQUE';

export type NoteType = 'INTERNE' | 'PUBLIC';



//Interfaces


export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
    refreshToken: string;
}

export interface RegisterRequest{
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    password: string;
    confirmPassword: string;
    termsAccepted: boolean;
}

// ============================================
// AUTH
// ============================================

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

// ============================================
// USERS
// ============================================

export interface UserResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: Role;
  status: AccountStatus;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phone: string;
}

export interface CreateInternalUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: 'AGENT' | 'SUPERVISEUR' | 'ADMINISTRATEUR';
}

export interface AdminUpdateUserRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  role?: Role;
}

export interface UserPage {
  content: UserResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ============================================
// CATEGORIES
// ============================================

export interface CategoryResponse {
  id: string;
  name: string;
  description: string;
  icon: string;
  defaultPriority: Priority;
  targetDelayHours: number;
  active: boolean;
}

export interface CategoryRequest {
  name: string;
  description: string;
  icon: string;
  defaultPriority: Priority;
  targetDelayHours: number;
}

// ============================================
// REPORTS
// ============================================

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
  district: string;
  municipality: string;
  photos: PhotoResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface ReportDetailResponse extends ReportResponse {
  citizen: UserResponse;
  activeIntervention: InterventionResponse | null;
  history: HistoryResponse[];
}

export interface ReportPage {
  content: ReportResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CancelReportRequest {
  reason: string;
}

// ============================================
// PHOTOS
// ============================================

export interface PhotoResponse {
  id: string;
  url: string;
  description: string;
  order: number;
  createdAt: string;
}

// ============================================
// INTERVENTIONS (Module 2 - prêt mais non utilisé en S2)
// ============================================

export type InterventionStatus = 'AFFECTEE' | 'EN_COURS' | 'RESOLUE' | 'REAFFECTEE' | 'INTERROMPUE';

export interface InterventionResponse {
  id: string;
  reportId: string;
  agent: UserResponse;
  status: InterventionStatus;
  assignedAt: string;
  startedAt: string | null;
  resolvedAt: string | null;
  instruction: string;
  resolutionComment: string;
  proofs: PhotoResponse[];
}

export interface AssignReportRequest {
  agentId: string;
  instruction?: string;
}

export interface ReassignReportRequest {
  newAgentId: string;
  reason: string;
}

// ============================================
// HISTORY & NOTES
// ============================================

export interface HistoryResponse {
  id: string;
  previousStatus: ReportStatus | null;
  newStatus: ReportStatus;
  comment: string;
  actor: UserResponse;
  changedAt: string;
}

export interface NoteResponse {
  id: string;
  content: string;
  type: NoteType;
  author: UserResponse;
  createdAt: string;
}

export interface CreateNoteRequest {
  content: string;
  type: NoteType;
}

// ============================================
// COMMON
// ============================================

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}
