import eneLogoDark from '../../assets/brand/ene-logo-dark.png';
import eneLogoLight from '../../assets/brand/ene-logo-light.png';

type BrandLogoProps = {
  variant: 'dark' | 'light';
  className?: string;
  alt?: string;
};

const logoByVariant = {
  dark: eneLogoDark,
  light: eneLogoLight,
} as const;

export function BrandLogo({ variant, className, alt = 'E&E Tech Solution' }: BrandLogoProps) {
  const classNames = ['brand-logo', className].filter(Boolean).join(' ');

  return <img className={classNames} src={logoByVariant[variant]} alt={alt} />;
}
