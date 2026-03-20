import { IconForum, IconSearch } from './icons.jsx'

const contestData = {
  hero: {
    titleZh: 'Hosted By Creator: @wuguigui',
    titleEn: 'Hosted By Creator: @wuguigui',
    subtitleZh: 'CyberBrick RC Cars Remix',
    subtitleEn: 'CyberBrick RC Cars Remix',
    badgeZh: '剩余 27 天结束',
    badgeEn: '27 days left',
    descZh: 'Be bold - and let your creativity run wild!',
    descEn: 'Be bold - and let your creativity run wild!',
    time: '2026/03/10 - 2026/04/12 UTC',
    entries: 30,
    participants: 24,
    views: 25325,
    image: 'https://picsum.photos/seed/contest-hero-main/1600/860',
  },
  secondary: [
    {
      id: 'easter',
      titleZh: 'Hop Into Creativity: Easter 2026 Design Contest',
      titleEn: 'Hop Into Creativity: Easter 2026 Design Contest',
      image: 'https://picsum.photos/seed/contest-easter/1200/620',
    },
    {
      id: 'april-fool',
      titleZh: "Expect the Unexpected: 2026 April Fools' Day Contest",
      titleEn: "Expect the Unexpected: 2026 April Fools' Day Contest",
      image: 'https://picsum.photos/seed/contest-april-fools/1200/620',
    },
  ],
}

function t(zh, zhText, enText) {
  return zh ? zhText : enText
}

export default function ContestBoard({ locale = 'zh', query = '', setQuery }) {
  const zh = locale === 'zh'
  const controlled = typeof setQuery === 'function'
  const hero = contestData.hero

  return (
    <section className="px-6 pb-8 pt-2 [zoom:75%]">
      <header className="sticky top-0 z-40 rounded-3xl border border-slate-200 bg-white px-5 py-2.5">
        <div className="flex items-center gap-3">
          <IconSearch className="h-5 w-5 text-slate-400" />
          <input
            className="w-full bg-transparent text-sm text-slate-900 outline-none placeholder:text-slate-400"
            placeholder={t(zh, '搜索模型、用户、收藏夹和动态', 'Search models, users, collections and feeds')}
            value={controlled ? query : undefined}
            onChange={controlled ? (event) => setQuery(event.target.value) : undefined}
          />
          <button
            type="button"
            className="rounded-full p-1.5 text-slate-500 hover:bg-slate-100"
            aria-label={t(zh, '社区入口', 'Community Entry')}
          >
            <IconForum className="h-5 w-5" />
          </button>
        </div>
      </header>

      <div className="mt-6 text-center">
        <h2 className="text-5xl font-bold text-slate-900">{t(zh, '设计竞赛', 'Design Contest')}</h2>
      </div>

      <div className="mx-auto mt-7 grid max-w-5xl grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-none border border-slate-300 bg-white px-6 py-4 text-left">
          <h3 className="text-2xl font-semibold text-slate-900">{t(zh, '竞赛预告', 'Contest Preview')}</h3>
          <p className="mt-3 text-lg text-slate-700">{t(zh, '敬请关注最新的竞赛预告!', 'Stay tuned for new contest previews!')}</p>
        </div>
        <div className="rounded-none border border-slate-300 bg-white px-6 py-4 text-center">
          <h3 className="text-3xl font-semibold leading-snug text-slate-900">
            Hosted By Creator: @Pi-Guy - Filament Recycling Design Contest
          </h3>
          <p className="mt-3 text-lg text-slate-600">© 2026-03-24 UTC</p>
          <a href="#" className="mt-3 inline-block text-lg font-semibold text-emerald-600 hover:text-emerald-700">
            {t(zh, '查看主题和奖品', 'View Topic & Rewards')}
          </a>
        </div>
      </div>

      <article className="mt-7 overflow-hidden border border-slate-200 bg-white xl:grid xl:grid-cols-[minmax(0,1fr)_370px]">
        <div className="relative min-h-[440px]">
          <img
            src={hero.image}
            alt={hero.subtitleEn}
            className="h-full min-h-[440px] w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-black/35 via-black/10 to-transparent" />
          <div className="absolute left-8 top-8 inline-flex items-center rounded-md bg-white/70 px-2 py-1 text-sm font-semibold text-slate-900">
            3D
          </div>
          <div className="absolute bottom-10 left-8 max-w-[640px]">
            <h3 className="text-6xl font-bold leading-tight text-slate-900">
              {zh ? hero.titleZh : hero.titleEn}
            </h3>
            <p className="mt-2 text-6xl font-bold leading-tight text-slate-900">
              {zh ? hero.subtitleZh : hero.subtitleEn}
            </p>
          </div>
        </div>

        <div className="flex h-full flex-col">
          <div className="px-6 py-7">
            <h4 className="line-clamp-2 text-5xl font-bold leading-tight text-slate-900">
              Hosted By Creator: @wuguigui
            </h4>
            <p className="mt-5 inline-flex items-center rounded-full border border-rose-300 px-3 py-1 text-lg font-semibold text-rose-600">
              {zh ? hero.badgeZh : hero.badgeEn}
            </p>
            <p className="mt-4 text-3xl leading-relaxed text-slate-700">{zh ? hero.descZh : hero.descEn}</p>
          </div>

          <div className="mt-auto border-t border-slate-200 px-6 py-5 text-lg text-slate-600">
            <p>{hero.time}</p>
            <div className="mt-3 flex items-center gap-6">
              <span>{hero.entries}</span>
              <span>{hero.participants}</span>
              <span>{hero.views}</span>
            </div>
          </div>
        </div>
      </article>

      <div className="mt-7 grid grid-cols-1 gap-4 xl:grid-cols-2">
        {contestData.secondary.map((item) => (
          <article key={item.id} className="group relative min-h-[220px] overflow-hidden">
            <img
              src={item.image}
              alt={item.titleEn}
              className="h-full min-h-[220px] w-full object-cover transition duration-300 group-hover:scale-[1.02]"
            />
            <div className="absolute inset-0 bg-gradient-to-r from-black/45 via-black/15 to-transparent" />
            <div className="absolute left-8 top-7 inline-flex items-center rounded-md bg-white/70 px-2 py-1 text-sm font-semibold text-slate-900">
              3D
            </div>
            <div className="absolute bottom-8 left-8 max-w-[72%] text-5xl font-bold leading-tight text-white">
              {zh ? item.titleZh : item.titleEn}
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
