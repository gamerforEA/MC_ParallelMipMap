package com.gamerforea.parallelmipmap;

import java.util.Iterator;
import java.util.concurrent.Callable;

import cpw.mods.fml.common.ProgressManager.ProgressBar;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;

public class ParallelUtils
{
	public static void generateMipMaps_MultiThread(final Iterator<TextureAtlasSprite> iterator, final ProgressBar bar, final int mipmapLevels)
	{
		Thread[] workers = new Thread[Runtime.getRuntime().availableProcessors()];

		for (int workerId = 0; workerId < workers.length; workerId++)
		{
			Thread worker = new Thread()
			{
				@Override
				public void run()
				{
					while (true)
					{
						TextureAtlasSprite sprite = null;

						synchronized (iterator)
						{
							if (iterator.hasNext()) sprite = iterator.next();
							else return;
						}

						final TextureAtlasSprite finalSprite = sprite;
						try
						{
							finalSprite.generateMipmaps(mipmapLevels);
							synchronized (bar)
							{
								bar.step(finalSprite.getIconName());
							}
						}
						catch (Throwable throwable)
						{
							CrashReport report = CrashReport.makeCrashReport(throwable, "Applying mipmap");
							CrashReportCategory category = report.makeCategory("Sprite being mipmapped");
							category.addCrashSectionCallable("Sprite name", new Callable<String>()
							{
								@Override
								public String call()
								{
									return finalSprite.getIconName();
								}
							});
							category.addCrashSectionCallable("Sprite size", new Callable<String>()
							{
								@Override
								public String call()
								{
									return finalSprite.getIconWidth() + " x " + finalSprite.getIconHeight();
								}
							});
							category.addCrashSectionCallable("Sprite frames", new Callable<String>()
							{
								@Override
								public String call()
								{
									return finalSprite.getFrameCount() + " frames";
								}
							});
							category.addCrashSection("Mipmap levels", mipmapLevels);
							throw new ReportedException(report);
						}
					}
				}
			};
			worker.setDaemon(true);
			worker.setName("MipMap worker #" + workerId);
			worker.start();
			workers[workerId] = worker;
		}

		for (Thread worker : workers)
			try
			{
				worker.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
	}
}