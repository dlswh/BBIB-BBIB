type LandingFeatureCardProps = {
  title: string;
  label: string;
  description: string;
  image: string;
  imageAlt: string;
};

export function LandingFeatureCard({ title, label, description, image, imageAlt }: LandingFeatureCardProps) {
  return (
    <article className="landing-feature-card">
      <img className="landing-feature-image" src={image} alt={imageAlt} />
      <div className="landing-feature-copy">
        <span>{label}</span>
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </article>
  );
}
