package com.nostra13.universalimageloader.core;

import java.io.File;
import java.util.concurrent.ThreadFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;

import com.nostra13.universalimageloader.cache.disc.DiscCacheAware;
import com.nostra13.universalimageloader.cache.disc.impl.FileCountLimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.MemoryCacheAware;
import com.nostra13.universalimageloader.cache.memory.impl.FuzzyKeyMemoryCache;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.MemoryCacheKeyUtil;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.core.download.URLConnectionImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

/**
 * Presents configuration for {@link ImageLoader}
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageLoader
 * @see MemoryCacheAware
 * @see DiscCacheAware
 * @see DisplayImageOptions
 * @see ImageDownloader
 * @see FileNameGenerator
 */
public final class ImageLoaderConfiguration {

	final int maxImageWidthForMemoryCache;
	final int maxImageHeightForMemoryCache;
	final int threadPoolSize;
	final boolean handleOutOfMemory;
	final MemoryCacheAware<String, Bitmap> memoryCache;
	final DiscCacheAware discCache;
	final DisplayImageOptions defaultDisplayImageOptions;
	final ThreadFactory displayImageThreadFactory;
	final boolean loggingEnabled;
	final ImageDownloader downloader;

	private ImageLoaderConfiguration(final Builder builder) {
		maxImageWidthForMemoryCache = builder.maxImageWidthForMemoryCache;
		maxImageHeightForMemoryCache = builder.maxImageHeightForMemoryCache;
		threadPoolSize = builder.threadPoolSize;
		handleOutOfMemory = builder.handleOutOfMemory;
		discCache = builder.discCache;
		memoryCache = builder.memoryCache;
		defaultDisplayImageOptions = builder.defaultDisplayImageOptions;
		loggingEnabled = builder.loggingEnabled;
		downloader = builder.downloader;
		displayImageThreadFactory = new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setPriority(builder.threadPriority);
				return t;
			}
		};
	}

	/**
	 * Creates default configuration for {@link ImageLoader} <br />
	 * <b>Default values:</b>
	 * <ul>
	 * <li>maxImageWidthForMemoryCache = {@link Builder#DEFAULT_MAX_IMAGE_WIDTH this}</li>
	 * <li>maxImageHeightForMemoryCache = {@link Builder#DEFAULT_MAX_IMAGE_HEIGHT this}</li>
	 * <li>httpConnectTimeout = {@link Builder#DEFAULT_HTTP_CONNECT_TIMEOUT this}</li>
	 * <li>httpReadTimeout = {@link Builder#DEFAULT_HTTP_READ_TIMEOUT this}</li>
	 * <li>threadPoolSize = {@link Builder#DEFAULT_THREAD_POOL_SIZE this}</li>
	 * <li>threadPriority = {@link Builder#DEFAULT_THREAD_PRIORITY this}</li>
	 * <li>allow to cache different sizes of image in memory</li>
	 * <li>memoryCache = {@link com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache
	 * UsingFreqLimitedCache} with limited memory cache size ( {@link Builder#DEFAULT_MEMORY_CACHE_SIZE this} bytes)</li>
	 * <li>discCache = {@link com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache UnlimitedDiscCache}</li>
	 * <li>defaultDisplayImageOptions = {@link DisplayImageOptions#createSimple() Simple options}</li>
	 * </ul>
	 * */
	public static ImageLoaderConfiguration createDefault(Context context) {
		return new Builder(context).build();
	}

	/**
	 * Builder for {@link ImageLoaderConfiguration}
	 * 
	 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
	 */
	public static class Builder {

		private static final String WARNING_OVERLAP_MEMORY_CACHE_SIZE = "This method's call overlaps memoryCacheSize() method call";
		private static final String WARNING_MEMORY_CACHE_ALREADY_SET = "You already have set memory cache. This method call will make no effect.";
		private static final String WARNING_OVERLAP_DISC_CACHE_SIZE = "This method's call overlaps discCacheSize() method call";
		private static final String WARNING_OVERLAP_DISC_CACHE_FILE_COUNT = "This method's call overlaps discCacheFileCount() method call";
		private static final String WARNING_OVERLAP_DISC_CACHE_FILE_NAME_GENERATOR = "This method's call overlaps discCacheFileNameGenerator() method call";
		private static final String WARNING_DISC_CACHE_ALREADY_SET = "You already have set disc cache. This method call will make no effect.";
		private static final String WARNING_OVERLAP_CONNECT_TIMEOUT = "This method's call overlaps httpConnectTimeout() method call";
		private static final String WARNING_OVERLAP_READ_TIMEOUT = "This method's call overlaps httpReadTimeout() method call";
		private static final String WARNING_DOWNLOADER_ALREADY_SET = "You already have set image downloader. This method call will make no effect.";

		/** {@value} milliseconds */
		public static final int DEFAULT_HTTP_CONNECT_TIMEOUT = 5000;
		/** {@value} milliseconds */
		public static final int DEFAULT_HTTP_READ_TIMEOUT = 20000;
		/** {@value} */
		public static final int DEFAULT_THREAD_POOL_SIZE = 5;
		/** {@value} */
		public static final int DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY - 1;
		/** {@value} bytes */
		public static final int DEFAULT_MEMORY_CACHE_SIZE = 2000000;

		private Context context;

		private int maxImageWidthForMemoryCache = 0;
		private int maxImageHeightForMemoryCache = 0;
		private int httpConnectTimeout = DEFAULT_HTTP_CONNECT_TIMEOUT;
		private int httpReadTimeout = DEFAULT_HTTP_READ_TIMEOUT;
		private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
		private int threadPriority = DEFAULT_THREAD_PRIORITY;
		private boolean allowCacheImageMultipleSizesInMemory = true;
		private boolean handleOutOfMemory = true;
		private int memoryCacheSize = DEFAULT_MEMORY_CACHE_SIZE;
		private MemoryCacheAware<String, Bitmap> memoryCache = null;
		private int discCacheSize = 0;
		private int discCacheFileCount = 0;
		private FileNameGenerator discCacheFileNameGenerator = null;
		private ImageDownloader downloader = null;
		private DiscCacheAware discCache = null;
		private boolean loggingEnabled = false;

		private DisplayImageOptions defaultDisplayImageOptions = null;

		public Builder(Context context) {
			this.context = context;
		}

		/**
		 * Sets maximum image width which will be used for memory saving during decoding an image to
		 * {@link android.graphics.Bitmap Bitmap}.<br />
		 * Default value - device's screen width
		 * */
		public Builder maxImageWidthForMemoryCache(int maxImageWidthForMemoryCache) {
			this.maxImageWidthForMemoryCache = maxImageWidthForMemoryCache;
			return this;
		}

		/**
		 * Sets maximum image height which will be used for memory saving during decoding an image to
		 * {@link android.graphics.Bitmap Bitmap}.<br />
		 * Default value - device's screen height
		 * */
		public Builder maxImageHeightForMemoryCache(int maxImageHeightForMemoryCache) {
			this.maxImageHeightForMemoryCache = maxImageHeightForMemoryCache;
			return this;
		}

		/**
		 * Sets timeout for HTTP connection establishment (during image loading).<br />
		 * Default value - {@link #DEFAULT_HTTP_CONNECT_TIMEOUT this}
		 * */
		public Builder httpConnectTimeout(int timeout) {
			if (downloader != null) Log.w(ImageLoader.TAG, WARNING_DOWNLOADER_ALREADY_SET);

			httpConnectTimeout = timeout;
			return this;
		}

		/**
		 * Sets timeout for HTTP reading (during image loading).<br />
		 * Default value - {@link #DEFAULT_HTTP_READ_TIMEOUT this}
		 * */
		public Builder httpReadTimeout(int timeout) {
			if (downloader != null) Log.w(ImageLoader.TAG, WARNING_DOWNLOADER_ALREADY_SET);

			httpReadTimeout = timeout;
			return this;
		}

		/**
		 * Sets thread pool size for image display tasks.<br />
		 * Default value - {@link #DEFAULT_THREAD_POOL_SIZE this}
		 * */
		public Builder threadPoolSize(int threadPoolSize) {
			this.threadPoolSize = threadPoolSize;
			return this;
		}

		/**
		 * Sets the priority for image loading threads. Must be <b>NOT</b> greater than {@link Thread#MAX_PRIORITY} or
		 * less than {@link Thread#MIN_PRIORITY}<br />
		 * Default value - {@link #DEFAULT_THREAD_PRIORITY this}
		 * */
		public Builder threadPriority(int threadPriority) {
			if (threadPriority < Thread.MIN_PRIORITY) {
				this.threadPriority = Thread.MIN_PRIORITY;
			} else {
				if (threadPriority > Thread.MAX_PRIORITY) {
					threadPriority = Thread.MAX_PRIORITY;
				} else {
					this.threadPriority = threadPriority;
				}
			}
			return this;
		}

		/**
		 * When you display an image in a small {@link android.widget.ImageView ImageView} and later you try to display
		 * this image (from identical URL) in a larger {@link android.widget.ImageView ImageView} so decoded image of
		 * bigger size will be cached in memory as a previous decoded image of smaller size.<br />
		 * So <b>the default behavior is to allow to cache multiple sizes of one image in memory</b>. You can
		 * <b>deny</b> it by calling <b>this</b> method: so when some image will be cached in memory then previous
		 * cached size of this image (if it exists) will be removed from memory cache before.
		 * */
		public Builder denyCacheImageMultipleSizesInMemory() {
			this.allowCacheImageMultipleSizesInMemory = false;
			return this;
		}

		/**
		 * ImageLoader try clean memory and re-display image it self when {@link OutOfMemoryError} occurs. You can
		 * switch off this feature by this method and process error by your way (you can know that
		 * {@link OutOfMemoryError} occurred if you got {@link FailReason#OUT_OF_MEMORY} in
		 * {@link ImageLoadingListener#onLoadingFailed(FailReason)}).
		 */
		public Builder offOutOfMemoryHandling() {
			this.handleOutOfMemory = false;
			return this;
		}

		/**
		 * Sets maximum memory cache size for {@link android.graphics.Bitmap bitmaps} (in bytes).<br />
		 * Default value - {@link #DEFAULT_MEMORY_CACHE_SIZE this}<br />
		 * <b>NOTE:</b> If you use this method then
		 * {@link com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache UsingFreqLimitedCache}
		 * will be used as memory cache. You can use {@link #memoryCache(MemoryCacheAware)} method for introduction your
		 * own implementation of {@link MemoryCacheAware}.
		 */
		public Builder memoryCacheSize(int memoryCacheSize) {
			if (memoryCacheSize <= 0) throw new IllegalArgumentException("memoryCacheSize must be a positive number");
			if (memoryCache != null) Log.w(ImageLoader.TAG, WARNING_MEMORY_CACHE_ALREADY_SET);

			this.memoryCacheSize = memoryCacheSize;
			return this;
		}

		/**
		 * Sets memory cache for {@link android.graphics.Bitmap bitmaps}.<br />
		 * Default value - {@link com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache
		 * UsingFreqLimitedCache} with limited memory cache size (size = {@link #DEFAULT_MEMORY_CACHE_SIZE this})<br />
		 * <b>NOTE:</b> You can use {@link #memoryCacheSize(int)} method instead of this method to simplify memory cache
		 * tuning.
		 */
		public Builder memoryCache(MemoryCacheAware<String, Bitmap> memoryCache) {
			if (memoryCacheSize != DEFAULT_MEMORY_CACHE_SIZE) Log.w(ImageLoader.TAG, WARNING_OVERLAP_MEMORY_CACHE_SIZE);

			this.memoryCache = memoryCache;
			return this;
		}

		/**
		 * Sets maximum disc cache size for images (in bytes).<br />
		 * By default: disc cache is unlimited.<br />
		 * <b>NOTE:</b> If you use this method then
		 * {@link com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache TotalSizeLimitedDiscCache}
		 * will be used as disc cache. You can use {@link #discCache(DiscCacheAware)} method for introduction your own
		 * implementation of {@link DiscCacheAware}
		 */
		public Builder discCacheSize(int maxCacheSize) {
			if (maxCacheSize <= 0) throw new IllegalArgumentException("maxCacheSize must be a positive number");
			if (discCache != null) Log.w(ImageLoader.TAG, WARNING_DISC_CACHE_ALREADY_SET);
			if (discCacheFileCount > 0) Log.w(ImageLoader.TAG, WARNING_OVERLAP_DISC_CACHE_FILE_COUNT);

			this.discCacheSize = maxCacheSize;
			return this;
		}

		/**
		 * Sets maximum file count in disc cache directory.<br />
		 * By default: disc cache is unlimited.<br />
		 * <b>NOTE:</b> If you use this method then
		 * {@link com.nostra13.universalimageloader.cache.disc.impl.FileCountLimitedDiscCache FileCountLimitedDiscCache}
		 * will be used as disc cache. You can use {@link #discCache(DiscCacheAware)} method for introduction your own
		 * implementation of {@link DiscCacheAware}
		 */
		public Builder discCacheFileCount(int maxFileCount) {
			if (maxFileCount <= 0) throw new IllegalArgumentException("maxFileCount must be a positive number");
			if (discCache != null) Log.w(ImageLoader.TAG, WARNING_DISC_CACHE_ALREADY_SET);
			if (discCacheSize > 0) Log.w(ImageLoader.TAG, WARNING_OVERLAP_DISC_CACHE_SIZE);

			this.discCacheSize = 0;
			this.discCacheFileCount = maxFileCount;
			return this;
		}

		/**
		 * Sets name generator for files cached in disc cache.<br />
		 * Default value - {@link com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator HashCodeFileNameGenerator}
		 */
		public Builder discCacheFileNameGenerator(FileNameGenerator fileNameGenerator) {
			if (discCache != null) Log.w(ImageLoader.TAG, WARNING_DISC_CACHE_ALREADY_SET);

			this.discCacheFileNameGenerator = fileNameGenerator;
			return this;
		}

		/**
		 * Sets utility which will be responsible for downloading of image.<br />
		 * Default value - {@link com.nostra13.universalimageloader.core.download.URLConnectionImageDownloader DefaultImageDownloader}
		 * */
		public Builder imageDownloader(ImageDownloader imageDownloader) {
			if (httpConnectTimeout != DEFAULT_HTTP_CONNECT_TIMEOUT) Log.w(ImageLoader.TAG, WARNING_OVERLAP_CONNECT_TIMEOUT);
			if (httpReadTimeout != DEFAULT_HTTP_READ_TIMEOUT) Log.w(ImageLoader.TAG, WARNING_OVERLAP_READ_TIMEOUT);

			this.downloader = imageDownloader;
			return this;
		}

		/**
		 * Sets disc cache for images.<br />
		 * Default value - {@link com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache
		 * UnlimitedDiscCache}. Cache directory is defined by <b>
		 * {@link com.nostra13.universalimageloader.utils.StorageUtils#getCacheDirectory(Context)
		 * StorageUtils.getCacheDirectory(Context)}.<br />
		 */
		public Builder discCache(DiscCacheAware discCache) {
			if (discCacheSize > 0) Log.w(ImageLoader.TAG, WARNING_OVERLAP_DISC_CACHE_SIZE);
			if (discCacheFileCount > 0) Log.w(ImageLoader.TAG, WARNING_OVERLAP_DISC_CACHE_FILE_COUNT);
			if (discCacheFileNameGenerator != null) Log.w(ImageLoader.TAG, WARNING_OVERLAP_DISC_CACHE_FILE_NAME_GENERATOR);

			this.discCache = discCache;
			return this;
		}

		/**
		 * Sets default {@linkplain DisplayImageOptions display image options} for image displaying. These options will
		 * be used for every {@linkplain ImageLoader#displayImage(String, android.widget.ImageView) image display call}
		 * without passing custom {@linkplain DisplayImageOptions options}<br />
		 * Default value - {@link DisplayImageOptions#createSimple() Simple options}
		 */
		public Builder defaultDisplayImageOptions(DisplayImageOptions defaultDisplayImageOptions) {
			this.defaultDisplayImageOptions = defaultDisplayImageOptions;
			return this;
		}

		/** Enabled detail logging of {@link ImageLoader} work */
		public Builder enableLogging() {
			this.loggingEnabled = true;
			return this;
		}

		/** Builds configured {@link ImageLoaderConfiguration} object */
		public ImageLoaderConfiguration build() {
			initEmptyFiledsWithDefaultValues();
			return new ImageLoaderConfiguration(this);
		}

		private void initEmptyFiledsWithDefaultValues() {
			if (discCache == null) {
				if (discCacheFileNameGenerator == null) {
					discCacheFileNameGenerator = new HashCodeFileNameGenerator();
				}

				if (discCacheSize > 0) {
					File individualCacheDir = StorageUtils.getIndividualCacheDirectory(context);
					discCache = new TotalSizeLimitedDiscCache(individualCacheDir, discCacheFileNameGenerator, discCacheSize);
				} else if (discCacheFileCount > 0) {
					File individualCacheDir = StorageUtils.getIndividualCacheDirectory(context);
					discCache = new FileCountLimitedDiscCache(individualCacheDir, discCacheFileNameGenerator, discCacheFileCount);
				} else {
					File cacheDir = StorageUtils.getCacheDirectory(context);
					discCache = new UnlimitedDiscCache(cacheDir, discCacheFileNameGenerator);
				}
			}
			if (memoryCache == null) {
				memoryCache = new UsingFreqLimitedMemoryCache(memoryCacheSize);
			}
			if (!allowCacheImageMultipleSizesInMemory) {
				memoryCache = new FuzzyKeyMemoryCache<String, Bitmap>(memoryCache, MemoryCacheKeyUtil.createFuzzyKeyComparator());
			}
			if (downloader == null) {
				downloader = new URLConnectionImageDownloader(httpConnectTimeout, httpReadTimeout);
			}
			if (defaultDisplayImageOptions == null) {
				defaultDisplayImageOptions = DisplayImageOptions.createSimple();
			}
			DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
			if (maxImageWidthForMemoryCache == 0) {
				maxImageWidthForMemoryCache = displayMetrics.widthPixels;
			}
			if (maxImageHeightForMemoryCache == 0) {
				maxImageHeightForMemoryCache = displayMetrics.heightPixels;
			}
		}
	}
}
