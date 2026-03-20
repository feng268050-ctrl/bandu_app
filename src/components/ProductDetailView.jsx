function DetailSpec({ label, value }) {
  return (
    <div className="rounded-2xl bg-slate-50 p-4">
      <div className="text-sm text-slate-500">{label}</div>
      <div className="mt-1 text-base font-semibold text-slate-900">{value}</div>
    </div>
  )
}

export default function ProductDetailView({ item, onBack, locale = 'zh' }) {
  if (!item) return null
  const zh = locale === 'zh'

  return (
    <div className="min-h-screen bg-[#f3f4f6] text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-[1500px] items-center gap-3 px-6 py-4">
          <button
            onClick={onBack}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium hover:bg-slate-50"
          >
            {zh ? '返回列表' : 'Back to list'}
          </button>
          <span className="text-sm text-slate-500">{zh ? '产品详情页（演示）' : 'Product Detail (Demo)'}</span>
        </div>
      </header>

      <main className="mx-auto max-w-[1500px] px-6 py-8">
        <section className="grid grid-cols-1 gap-6 xl:grid-cols-5">
          <div className="xl:col-span-3">
            <div className="overflow-hidden rounded-[28px] bg-white ring-1 ring-slate-200">
              <img src={item.image} alt={item.title} className="h-[440px] w-full object-cover" />
            </div>
          </div>

          <div className="xl:col-span-2">
            <div className="rounded-[28px] bg-white p-6 ring-1 ring-slate-200">
              <div className="mb-3 flex flex-wrap gap-2">
                {(item.tags ?? []).map((tag) => (
                  <span
                    key={tag}
                    className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700"
                  >
                    {tag}
                  </span>
                ))}
              </div>

              <h1 className="text-3xl font-bold leading-tight text-slate-900">{item.title}</h1>
              <p className="mt-3 text-sm leading-7 text-slate-600">{item.description}</p>

              <div className="mt-6 grid grid-cols-3 gap-3">
                <div className="rounded-2xl bg-slate-50 p-4 text-center">
                  <p className="text-xs text-slate-500">{zh ? '收藏' : 'Likes'}</p>
                  <p className="mt-1 text-lg font-semibold text-slate-900">{item.likes ?? '--'}</p>
                </div>
                <div className="rounded-2xl bg-slate-50 p-4 text-center">
                  <p className="text-xs text-slate-500">{zh ? '浏览' : 'Views'}</p>
                  <p className="mt-1 text-lg font-semibold text-slate-900">{item.views ?? '--'}</p>
                </div>
                <div className="rounded-2xl bg-slate-50 p-4 text-center">
                  <p className="text-xs text-slate-500">{zh ? '评分' : 'Rating'}</p>
                  <p className="mt-1 text-lg font-semibold text-slate-900">{item.rating ?? '--'}</p>
                </div>
              </div>

              <div className="mt-6 space-y-2 text-sm text-slate-600">
                <p>{zh ? '作者：' : 'Author: '}{item.creator ?? (zh ? '未知' : 'Unknown')}</p>
                <p>{zh ? '分类：' : 'Category: '}{item.category ?? (zh ? '未分类' : 'Uncategorized')}</p>
                <p>{zh ? '发布时间：' : 'Published: '}{item.publishedAt ?? (zh ? '未设置' : 'Not set')}</p>
              </div>

              <div className="mt-6 flex flex-wrap gap-3">
                <button className="rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-slate-700">
                  {zh ? '查看文件' : 'View Files'}
                </button>
                <button className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50">
                  {zh ? '收藏作品' : 'Save Project'}
                </button>
              </div>
            </div>
          </div>
        </section>

        <section className="mt-6 grid grid-cols-1 gap-6 xl:grid-cols-5">
          <article className="rounded-[28px] bg-white p-6 ring-1 ring-slate-200 xl:col-span-3">
            <h2 className="text-2xl font-bold text-slate-900">{zh ? '项目说明' : 'Project Notes'}</h2>
            <p className="mt-3 text-sm leading-8 text-slate-600">
              {item.description}
            </p>
            <p className="mt-3 text-sm leading-8 text-slate-600">
              {zh
                ? '当前是一个假详情页示例，已经包含基础信息区、数据区、参数区和操作按钮。你后续可以继续扩展评论区、版本日志、相关推荐和下载面板。'
                : 'This demo page includes basic info, metrics, specs and action buttons. You can extend it with comments, changelog, recommendations and download panels.'}
            </p>
          </article>

          <article className="rounded-[28px] bg-white p-6 ring-1 ring-slate-200 xl:col-span-2">
            <h2 className="text-2xl font-bold text-slate-900">{zh ? '参数信息' : 'Specifications'}</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-1">
              <DetailSpec label={zh ? '预计打印时间' : 'Print Time'} value={item.specs?.printTime ?? '--'} />
              <DetailSpec label={zh ? '推荐材料' : 'Material'} value={item.specs?.material ?? '--'} />
              <DetailSpec label={zh ? '难度等级' : 'Difficulty'} value={item.specs?.difficulty ?? '--'} />
              <DetailSpec label={zh ? '模型尺寸' : 'Model Size'} value={item.specs?.size ?? '--'} />
            </div>
          </article>
        </section>
      </main>
    </div>
  )
}
