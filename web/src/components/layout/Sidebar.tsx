import {
  Activity,
  ChevronRight,
  LayoutDashboard,
  Search,
  Settings,
  User,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

// workers 제거, operators로 통일
export type SidebarNavKey = 'overview' | 'machines' | 'logs' | 'operators' | 'settings';

type NavItem = {
  id: SidebarNavKey;
  label: string;
  icon: LucideIcon;
  arrow?: boolean;
};

type SidebarProps = {
  activeItem?: SidebarNavKey;
  onSelect?: (item: SidebarNavKey) => void;
};

export function Sidebar({ activeItem = 'overview', onSelect }: SidebarProps) {
  const navItems: NavItem[] = [
    { id: 'overview',   label: '전체 현황', icon: LayoutDashboard },
    { id: 'machines',   label: '장비 상태', icon: Activity },
    { id: 'logs',       label: '로그 검색', icon: Search },
    { id: 'operators',  label: '작업자',    icon: User },
    { id: 'settings',   label: '설정',      icon: Settings, arrow: true },
  ];

  return (
    <aside className="sidebar">
      <nav className="nav-list" aria-label="주 메뉴">
        {navItems.map((item) => {
          const isActive = item.id === activeItem;
          return (
            <button
              className={`nav-item ${isActive ? 'active' : ''}`}
              key={item.id}
              type="button"
              aria-current={isActive ? 'page' : undefined}
              onClick={() => onSelect?.(item.id)}
            >
              <item.icon size={22} aria-hidden="true" />
              <span>{item.label}</span>
              {item.arrow ? <ChevronRight size={16} aria-hidden="true" /> : null}
            </button>
          );
        })}
      </nav>
    </aside>
  );
}