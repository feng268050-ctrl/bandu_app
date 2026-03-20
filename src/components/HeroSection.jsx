import { exploreCards, heroSlide } from '../data/mockData'

const exploreDescEn = {
  makerlab: 'Create faster with flexible templates',
  parts: 'Curated parts, kits and materials',
  crowdfunding: 'Turn ideas into real products',
  cyberbrick: 'Build smart, collaborate globally',
}

export default function HeroSection({ locale = 'zh' }) {
  const zh = locale === 'zh'

  return (
    <section className="grid grid-cols-1 gap-6 px-6 lg:grid-cols-[1.6fr_1fr]">
      <article className="relative overflow-hidden rounded-[28px] bg-slate-900">
        <img src={heroSlide.image} alt="hero banner" className="h-[350px] w-full object-cover" />

        <div className="absolute inset-x-0 bottom-0 flex items-end justify-between bg-gradient-to-t from-black/70 via-black/35 to-transparent p-6">
          <h2 className="text-4xl font-bold tracking-tight text-white lg:text-5xl">{heroSlide.title}</h2>

          <div className="mb-1 flex items-center gap-2">
            {[0, 1, 2, 3, 4].map((item, index) => (
              <span
                key={item}
                className={`h-2.5 rounded-full ${
                  index === 4 ? 'w-8 bg-white' : 'w-2.5 bg-white/35'
                }`}
              />
            ))}
          </div>
        </div>
      </article>

      <aside className="rounded-[28px] bg-gradient-to-br from-lime-100 via-green-50 to-emerald-100 p-5">
        <h3 className="mb-4 text-3xl font-bold tracking-tight text-slate-900">
          {zh ? '探索更多' : 'Discover More'}
        </h3>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-2">
          {exploreCards.map((item) => (
            <article
              key={item.id}
              className="flex items-center justify-between rounded-3xl bg-white p-4 shadow-[0_1px_0_rgba(15,23,42,0.05)]"
            >
              <div className="min-w-0 pr-2">
                <h4 className="truncate text-xl font-semibold text-slate-900">{item.title}</h4>
                <p className="mt-1 text-sm leading-6 text-slate-500">
                  {zh ? item.desc : exploreDescEn[item.id] ?? item.desc}
                </p>
              </div>

              <div
                className={`flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br ${item.iconBg} text-sm font-bold text-slate-700`}
              >
                {item.iconText}
              </div>
            </article>
          ))}
        </div>
      </aside>
    </section>
  )
}
