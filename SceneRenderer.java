import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F6;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F7;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_STICKY_KEYS;
import static org.lwjgl.glfw.GLFW.GLFW_AUTO_ICONIFY;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F8;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWaitEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_RELAXED_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.vulkan.VKCapabilitiesInstance;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

public class SceneRenderer extends VulkanApplication<SceneRendererPerFrameThreadObject>{

	
	protected static final boolean RENDERDOC_ENABLED = false;
	protected static final boolean DEBUG_MODE = true;
	
	protected final static int INDEX_TYPE = VK_INDEX_TYPE_UINT32;
	
	private final int preferredSwapSurfaceFormat = VK_FORMAT_B8G8R8A8_UNORM;
	private final int preferredSwapSurfaceColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
	private final int[] preferredSwapPresentModeRanking = new int[] {
			VK_PRESENT_MODE_MAILBOX_KHR,//Triplebuffering
			VK_PRESENT_MODE_IMMEDIATE_KHR,//No Buffering
			VK_PRESENT_MODE_FIFO_KHR,//Normal VerticalSync
			VK_PRESENT_MODE_FIFO_RELAXED_KHR//bad Version of Vsync...With tearing
	};
	
	private static final String[] requestedValidationLayers;
	private static final String[] requestedPhysicalDeviceExtensions;
	private static final VkPhysicalDeviceFeatures2 requestedPhysicalDeviceFeaturesExtensionChain;
	static {
		if(RENDERDOC_ENABLED) {
			requestedPhysicalDeviceExtensions = new String[] {
					VK_KHR_SWAPCHAIN_EXTENSION_NAME,
			};
			requestedValidationLayers=new String[] {
					"VK_LAYER_RENDERDOC_Capture",
			};
			requestedPhysicalDeviceFeaturesExtensionChain = VkPhysicalDeviceFeatures2.calloc()
					.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2);
		}else {
			requestedPhysicalDeviceExtensions = new String[] {
					VK_KHR_SWAPCHAIN_EXTENSION_NAME,
			};
			requestedValidationLayers=new String[] {
					"VK_LAYER_LUNARG_standard_validation"
			};
			requestedPhysicalDeviceFeaturesExtensionChain =null;
		}
	}
	private static final String[] requiredPhysicalDeviceExtensions = new String[] {
			VK_KHR_SWAPCHAIN_EXTENSION_NAME,
	};
	protected static final HashMap<String, Boolean> supportedPhysicalDeviceExtensions = new HashMap<String, Boolean>(requestedPhysicalDeviceExtensions.length);
	
	private final VkPhysicalDeviceProperties requestedPhysicalDeviceProperties = VkPhysicalDeviceProperties.calloc();
	private static final VkPhysicalDeviceFeatures requestedPhysicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc();
	
	private static final HashSet<Integer> debouncedKeys = new HashSet<Integer>();
	static {
		debouncedKeys.add(new Integer(GLFW_KEY_F6));
		debouncedKeys.add(new Integer(GLFW_KEY_F7));
		debouncedKeys.add(new Integer(GLFW_KEY_F8));
	}
	
	protected long window;
	protected LongBuffer surface = memAllocLong(1);
	protected LongBuffer swapChain = memAllocLong(1).put(0, VK_NULL_HANDLE);
	private LongBuffer swapChainImages;
	protected int swapChainImageFormat;
	protected VkExtent2D swapChainExtent;
	protected LongBuffer imageAvailableSemaphores;
	protected ArrayBlockingQueue<Long> imageAvailableSemaphoresList;
	
	protected LongBuffer colorImages;
	protected LongBuffer colorImagesMemory;
	protected int colorImageFormat;
	
	protected PresentThread presentThread;
	protected int currentAcquiredSwapChainImagesCount;
	protected final Object waitAcquiredSwapChainImagesMonitorObject = new Object();
	protected volatile boolean framebufferResized = false;
	protected volatile boolean swapChainMustBeRecreated = false;
	
	protected int maxAcquiredSwapChainImagesCount;
	
	public SceneRenderer() {
		super(glfwGetRequiredInstanceExtensions(), requestedValidationLayers, requestedPhysicalDeviceExtensions, requestedPhysicalDeviceFeatures, requestedPhysicalDeviceFeaturesExtensionChain);
	}
	
	
	protected void init(){
		initWindow();
		initVulkan();
	}
	
	public void run() {
		init();
		mainLoop();
		cleanup();
	}
	
	private void initWindow() {
		glfwInit();
		
		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);//Tell GLFW we don't use OpenGl
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
		
		glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_TRUE);
		
		window = glfwCreateWindow(600, 800, "Vulkan", NULL, NULL);
		glfwSetFramebufferSizeCallback(window, new framebufferResizeCallback());
		glfwSetKeyCallback(window, new keyCallback());
		glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));
		
		glfwSetInputMode(window, GLFW_STICKY_KEYS, GLFW_TRUE);
	}
	
	protected void initVulkan() {
		createInstance(requestedValidationLayers, glfwGetRequiredInstanceExtensions());
		setupDebugMessenger();
		createSurface();
		this.physicalDevice = pickPhysicalDevice();
		createLogicalDevice(requestedValidationLayers, requestedPhysicalDeviceExtensions, requestedPhysicalDeviceFeatures, requestedPhysicalDeviceFeaturesExtensionChain, supportedPhysicalDeviceExtensions);
		createCommandPools();
		createSwapChain();
		createColorResources();
		createSyncObjects();
		setupPresentThread();
		this.perFrameThreadObjects = createPerFrameThreadObjects();
	}
	
	private void createSurface() {
		int err = glfwCreateWindowSurface(instance, window, null, surface);
		
		if(err!=VK_SUCCESS) {
			throw new AssertionError("Failed to create Surface: " + err);
		}
	}
	
	@Override
	protected VkPhysicalDevice pickPhysicalDevice() {
		VkPhysicalDevice ret = null;
		int[] physicalDeviceCount = new int[1];
		
		vkEnumeratePhysicalDevices(instance, physicalDeviceCount, null);
		if(physicalDeviceCount[0]<=0) {
			throw new RuntimeException("Faild to find GPU with Vulkan support!");
		}
		PointerBuffer physicalDevicesAvailable = memAllocPointer(physicalDeviceCount[0]);
		vkEnumeratePhysicalDevices(instance, physicalDeviceCount, physicalDevicesAvailable);
		
		boolean foundSuitablePhysicalDevice = false;
	
		for(int i=0;i<physicalDeviceCount[0];++i) {
			physicalDevicesAvailable.position(i);
			VkPhysicalDevice current = new VkPhysicalDevice(physicalDevicesAvailable.get(),instance);
			if(isPhysicalDeviceSuitable(current)) {
				ret=current;
				foundSuitablePhysicalDevice = true;
				break;
			}
		}
		
		if(!foundSuitablePhysicalDevice) {
			throw new RuntimeException("Failed to find suitable GPU!");
		}
		if(physicalDevicesAvailable!=null) {
			memFree(physicalDevicesAvailable);
		}
		return ret;
	}
	
	private void createSwapChain() {
		VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(getSupportedPhysicalDeviceFormats(physicalDevice, new int[1]));
		int presentMode = chooseSwapPresentMode(getSupportedPhysicalDevicePresentModes(physicalDevice, new int[1]));
		
		VkSurfaceCapabilitiesKHR surfaceCapabilities = getSupportedSurfaceCapabilities(physicalDevice);
		VkExtent2D extent = chooseSwapExtent(surfaceCapabilities);
		
		swapChainExtent=extent;
		swapChainImageFormat=surfaceFormat.format();
		
		int imageCount = surfaceCapabilities.minImageCount()+1;//Request one more than the minimum to be safe
		
		if(surfaceCapabilities.maxImageCount()>0 && surfaceCapabilities.maxImageCount()<imageCount) {
			imageCount = surfaceCapabilities.maxImageCount();
		}
		
		VkSwapchainCreateInfoKHR swapChainCreateInfo = VkSwapchainCreateInfoKHR.calloc();
		swapChainCreateInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
		swapChainCreateInfo.surface(surface.get(0));
		
		swapChainCreateInfo.minImageCount(imageCount);
		swapChainCreateInfo.imageFormat(surfaceFormat.format());
		swapChainCreateInfo.imageColorSpace(surfaceFormat.colorSpace());
		swapChainCreateInfo.imageExtent(extent);
		swapChainCreateInfo.imageArrayLayers(1);//Just one Layer per Image cause it's not VR
		swapChainCreateInfo.imageUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT);
		
		if(!(queueFamilyIndices[0] == queueFamilyIndices[1])) {
			swapChainCreateInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
			IntBuffer queueFamilyIndicesBuffer = memAllocInt(2);
			queueFamilyIndicesBuffer.put(queueFamilyIndices[0]).put(queueFamilyIndices[1]).flip();
			swapChainCreateInfo.pQueueFamilyIndices(queueFamilyIndicesBuffer);
			memFree(queueFamilyIndicesBuffer);
		}else {
			swapChainCreateInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);//Better Performance...Create System later that only uses this Mode
			swapChainCreateInfo.pQueueFamilyIndices(null);
		}
		
		swapChainCreateInfo.preTransform(surfaceCapabilities.currentTransform());//Don't transform the images in the queue
		swapChainCreateInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);//Don't blend with other windows
		
		swapChainCreateInfo.presentMode(presentMode);
		swapChainCreateInfo.clipped(true);
		
		long oldSwapChain = swapChain.get(0);
		swapChain = memAllocLong(1);
		swapChainCreateInfo.oldSwapchain(oldSwapChain);

		int err = vkCreateSwapchainKHR(device, swapChainCreateInfo, null, swapChain);
		
		if(oldSwapChain != VK_NULL_HANDLE) {
			vkDestroySwapchainKHR(device, oldSwapChain, null);	
		}
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create SwapChain: " + err);
		}
		
		IntBuffer swapChainImageCount = memAllocInt(1);
		vkGetSwapchainImagesKHR(device, swapChain.get(0), swapChainImageCount, null);
		swapChainImages = memAllocLong(swapChainImageCount.get(0));
		vkGetSwapchainImagesKHR(device, swapChain.get(0), swapChainImageCount, swapChainImages);
		
		maxAcquiredSwapChainImagesCount = swapChainImageCount.get(0) - surfaceCapabilities.minImageCount();
		
		LongBuffer currentImage = memAllocLong(1);
		for(int i=0;i<swapChainImages.limit();++i) {
			currentImage.put(0, swapChainImages.get(i));
			transitionImageLayout(currentImage, swapChainImageFormat, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, 1, transferCommandPool, transferQueue);
		}
		
		memFree(currentImage);
		memFree(swapChainImageCount);
		swapChainCreateInfo.free();
	}
	
	private void createColorResources() {
		boolean allQueueFamiliesSame = true;
		int compareQueueFamily = queueFamilyIndices[0];
		for(int i=1;i<queueFamilyIndices.length;++i) {
			if(queueFamilyIndices[i]!=compareQueueFamily) {
				allQueueFamiliesSame=false;
				break;
			}
		}
		int sharingMode;
		if(allQueueFamiliesSame) {
			sharingMode = VK_SHARING_MODE_EXCLUSIVE;
		}else {
			sharingMode = VK_SHARING_MODE_CONCURRENT;
		}
		
		colorImages = memAllocLong(swapChainImages.limit());
		colorImagesMemory = memAllocLong(swapChainImages.limit());
		
		LongBuffer currentImage = memAllocLong(1);
		LongBuffer currentImageMemory = memAllocLong(1);
		
		int tiling = VK_IMAGE_TILING_OPTIMAL;
		colorImageFormat = swapChainImageFormat;
		int usage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
		int properties = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT;
		
		for(int i=0;i<swapChainImages.limit();++i) {
			createImage(swapChainExtent.width(), swapChainExtent.height(), 1, VK_SAMPLE_COUNT_1_BIT, colorImageFormat, tiling, usage, properties, currentImage, currentImageMemory, sharingMode,
					VK_IMAGE_TYPE_2D, 1);
			
			transitionImageLayout(currentImage, colorImageFormat, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_GENERAL, 1, 0, 1, transferCommandPool, transferQueue);
			
			colorImages.put(i, currentImage.get(0));
			colorImagesMemory.put(i, currentImageMemory.get(0));
		}
		memFree(currentImage);
		memFree(currentImageMemory);
	}
	
	@Override
	protected SceneRendererPerFrameThreadObject[] createPerFrameThreadObjects() {
		SceneRendererPerFrameThreadObject[] perFrameThreadObjects = new SceneRendererPerFrameThreadObject[swapChainImages.limit()];
		for(int i=0;i<swapChainImages.limit();++i) {
			perFrameThreadObjects[i] = new SceneRendererPerFrameThreadObject(this, swapChainImages.get(i), colorImages.get(i), i);
		}
		
		return perFrameThreadObjects;
	}
	
	private void createSyncObjects() {
		imageAvailableSemaphores = memAllocLong(swapChainImages.limit());
		imageAvailableSemaphoresList = new ArrayBlockingQueue<Long>(swapChainImages.limit());
		
		VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc();
		semaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
		
		LongBuffer currentSyncObject = memAllocLong(1);
		for(int i=0;i<swapChainImages.limit();++i) {
			
			int err = vkCreateSemaphore(device, semaphoreCreateInfo, null, currentSyncObject);
			
			if (err != VK_SUCCESS) {
	            throw new AssertionError("Failed to create Semaphores: " + err);
			}
			
			imageAvailableSemaphores.put(i, currentSyncObject.get(0));
			imageAvailableSemaphoresList.add(new Long(currentSyncObject.get(0)));
		}
		
		memFree(currentSyncObject);
		semaphoreCreateInfo.free();
	}
	
	protected void recreateSwapChain() {
		int[] width = new int[1];
		int[] height = new int[1];
		
		while(width[0] == 0 || height[0] == 0) {
			glfwGetFramebufferSize(window, width, height);
			glfwWaitEvents();
		}
		vkDeviceWaitIdle(device);
		
		cleanupSwapChain();
		
		createSwapChain();

		createColorResources();

		for(int i=0;i<swapChainImages.limit();++i) {
			synchronized(perFrameThreadObjects[i]) {
				perFrameThreadObjects[i].recreateSwapChain(swapChainImages.get(i), colorImages.get(i));
			}
		}
		this.swapChainMustBeRecreated = false;
	}
	
	private void cleanupSwapChain() {
		
		for(int i=0;i<swapChainImages.limit();++i) {
			synchronized(perFrameThreadObjects[i].waitingMonitorObject) {//Wait till all threads are in waiting mode
				boolean loop;
				synchronized(perFrameThreadObjects[i]) {
					loop = !perFrameThreadObjects[i].getWaiting();
				}
				while(loop) {
					try {
						perFrameThreadObjects[i].waitingMonitorObject.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(-1);
					}
					synchronized(perFrameThreadObjects[i]) {
						loop = !perFrameThreadObjects[i].getWaiting();
					}
				}
			}
		}
		
		while(presentThread.jobQueue.poll() != null) {
			
		}
		synchronized(presentThread.waitObject) {
			boolean loop;
			synchronized(presentThread) {
				loop = !presentThread.waiting;
			}
			while(loop) {
				try {
					presentThread.waitObject.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(-1);
				}
				synchronized(presentThread) {
					loop = !presentThread.waiting;
				}
			}
		}
		for(int i=0;i<swapChainImages.limit();++i) {
			synchronized(perFrameThreadObjects[i]) {
				perFrameThreadObjects[i].cleanupSwapChain();
			}
		}
		
		for(int i=0;i<colorImages.limit();++i) {
			vkDestroyImage(device, colorImages.get(i), null);
			vkFreeMemory(device, colorImagesMemory.get(i), null);
		}
		
		memFree(colorImages);
		memFree(colorImagesMemory);
	}
	
	@Override
	protected boolean isPhysicalDeviceSuitable(VkPhysicalDevice physicalDevice) {
		VkPhysicalDeviceProperties physicalDeviceProperties = VkPhysicalDeviceProperties.calloc();
		vkGetPhysicalDeviceProperties(physicalDevice, physicalDeviceProperties);
		
		VKCapabilitiesInstance physicalDeviceCapabilities = physicalDevice.getCapabilities();
		
		VkPhysicalDeviceFeatures physicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc();
		if(physicalDeviceCapabilities.vkGetPhysicalDeviceFeatures2 != VK_NULL_HANDLE) {
			VkPhysicalDeviceFeatures2 physicalDeviceFeatures2 = VkPhysicalDeviceFeatures2.calloc();
			physicalDeviceFeatures2.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2);
			vkGetPhysicalDeviceFeatures2(physicalDevice, physicalDeviceFeatures2);
			physicalDeviceFeatures.free();
			physicalDeviceFeatures = physicalDeviceFeatures2.features();
		}else {
			vkGetPhysicalDeviceFeatures(physicalDevice, physicalDeviceFeatures);
		}
		
		//Check PhysicalDeviceExtensions
		int[] physicalDeviceExtensionCount = new int[1];
		vkEnumerateDeviceExtensionProperties(physicalDevice,(CharSequence)null,physicalDeviceExtensionCount,null);
		VkExtensionProperties.Buffer availablePhysicalDeviceExtensions = VkExtensionProperties.calloc(physicalDeviceExtensionCount[0]);
		vkEnumerateDeviceExtensionProperties(physicalDevice,(CharSequence)null,physicalDeviceExtensionCount,availablePhysicalDeviceExtensions);
		
		for(int i=0;i<requestedPhysicalDeviceExtensions.length;++i) {
			boolean currentAvailable=false;
			for(int j=0;j<physicalDeviceExtensionCount[0];++j) {
				availablePhysicalDeviceExtensions.position(j);
				if(requestedPhysicalDeviceExtensions[i].equals(availablePhysicalDeviceExtensions.extensionNameString())) {
					currentAvailable=true;
					break;
				}
			}
			if(!currentAvailable) {
				for(int j=0;j<requiredPhysicalDeviceExtensions.length;++j) {
					if(requiredPhysicalDeviceExtensions[j].equals(requestedPhysicalDeviceExtensions[i])) {
						return false;
					}
				}
				supportedPhysicalDeviceExtensions.put(requestedPhysicalDeviceExtensions[i], false);
			}else {
				supportedPhysicalDeviceExtensions.put(requestedPhysicalDeviceExtensions[i], true);
			}
		}
		
		//Check SwapChain support
		VkSurfaceCapabilitiesKHR supportedSurfaceCapabilities = getSupportedSurfaceCapabilities(physicalDevice);
		
		int[] supportedPhysicalDeviceFormatCount = new int[1];
		VkSurfaceFormatKHR.Buffer supportedPhysicalDeviceFormats = getSupportedPhysicalDeviceFormats(physicalDevice, supportedPhysicalDeviceFormatCount);

		
		int[] supportedPhysicalDevicePresentModeCount = new int[1];
		getSupportedPhysicalDevicePresentModes(physicalDevice, supportedPhysicalDevicePresentModeCount);
		
		
		if((supportedPhysicalDeviceFormatCount[0]==0)||(supportedPhysicalDevicePresentModeCount[0]==0)) {
			return false;
		}
		
		
		//Check available Queues
				int[] queueFamilyCount = new int[1];
				VkQueueFamilyProperties.Buffer queueFamilies = getQueueFamilies(physicalDevice, queueFamilyCount);
				boolean foundSuitableGraphicsQueueFamily = false;
				final int requiredGraphicsQueueFlagBits = VK_QUEUE_GRAPHICS_BIT;
				boolean foundSuitablePresentationQueueFamily = false;
				boolean foundSuitableTransferQueueFamily = false;
				final int requiredTransferQueueFlagBits = VK_QUEUE_TRANSFER_BIT;
				boolean foundSuitableComputeQueueFamily = false;
				final int requiredComputeQueueFlagBits = VK_QUEUE_COMPUTE_BIT;
				for(int i=0;i<queueFamilyCount[0]&&!foundSuitableGraphicsQueueFamily&&!foundSuitablePresentationQueueFamily&&!foundSuitableTransferQueueFamily;++i) {
					queueFamilies.position(i);
					//GraphicsQueue
					if(!foundSuitableGraphicsQueueFamily&&queueFamilies.queueCount() >0 && ((queueFamilies.queueFlags() & requiredGraphicsQueueFlagBits)==requiredGraphicsQueueFlagBits)) {
						foundSuitableGraphicsQueueFamily = true;
						queueFamilyIndices[0]=i;
					}
					//PresentationQueue
					int[] presentationSupportInt = new int[1];
					int err = vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface.get(0), presentationSupportInt);
					
					if (err != VK_SUCCESS) {
			            throw new AssertionError("Failed to create logical Device: " + err);
					}
					
					boolean presentationSupport = (presentationSupportInt[0]==VK_TRUE);
					if(!foundSuitablePresentationQueueFamily&&queueFamilies.queueCount() >0 && presentationSupport) {
						foundSuitablePresentationQueueFamily = true;
						queueFamilyIndices[1]=i;
					}
					
					//TransferQueue
					if(!foundSuitableTransferQueueFamily&&queueFamilies.queueCount() > 0 && ((queueFamilies.queueFlags() & requiredTransferQueueFlagBits)==requiredTransferQueueFlagBits)) {
						foundSuitableTransferQueueFamily = true;
						queueFamilyIndices[2]=i;
					}
					
					//ComputeQueue
					if(!foundSuitableComputeQueueFamily&&queueFamilies.queueCount() > 0 && ((queueFamilies.queueFlags() & requiredComputeQueueFlagBits)==requiredComputeQueueFlagBits)) {
						foundSuitableComputeQueueFamily = true;
						queueFamilyIndices[3]=i;
					}
				}
				
				physicalDeviceProperties.free();
				if(physicalDeviceCapabilities.vkGetPhysicalDeviceFeatures2 == VK_NULL_HANDLE) {
					physicalDeviceFeatures.free();
				}
				availablePhysicalDeviceExtensions.free();
				supportedSurfaceCapabilities.free();
				supportedPhysicalDeviceFormats.free();
				queueFamilies.free();
				return foundSuitableGraphicsQueueFamily&&foundSuitablePresentationQueueFamily&&foundSuitableTransferQueueFamily&&foundSuitableComputeQueueFamily;
	}
	
	private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer supportedSwapSurfaceFormats) {
		if(supportedSwapSurfaceFormats.limit()==1 && supportedSwapSurfaceFormats.get(0).format() == VK_FORMAT_UNDEFINED) {
			return createSwapSurfaceFormat(preferredSwapSurfaceFormat, preferredSwapSurfaceColorSpace);
		}else {
			for(int i=0;i<supportedSwapSurfaceFormats.limit();++i) {
				supportedSwapSurfaceFormats.position(i);
				if(supportedSwapSurfaceFormats.format() == preferredSwapSurfaceFormat && supportedSwapSurfaceFormats.colorSpace() == preferredSwapSurfaceColorSpace) {
					return supportedSwapSurfaceFormats.get(i);
				}
			}
			
			//Else do ranking or just return first
			return supportedSwapSurfaceFormats.get(0);
		}
	}
	
	private int chooseSwapPresentMode(int[] supportedSwapPresentModes) {
		for(int i=0;i<preferredSwapPresentModeRanking.length;++i) {
			for(int j=0;j<supportedSwapPresentModes.length;++j) {
				if(preferredSwapPresentModeRanking[i]==supportedSwapPresentModes[j]) {
					return preferredSwapPresentModeRanking[i];
				}
			}
		}
		
		return VK_PRESENT_MODE_IMMEDIATE_KHR;//Default is no Buffering
	}
	
	private VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR supportedSwapSurfaceCapabilities) {
		if(supportedSwapSurfaceCapabilities.currentExtent().width() == Integer.MAX_VALUE) {
			int[] width = new int[1];
			int[] height = new int[1];
			glfwGetFramebufferSize(window, width, height);
			
			VkExtent2D actuallExtent = VkExtent2D.calloc();
			actuallExtent.width(width[0]);
			actuallExtent.height(height[0]);
			
			actuallExtent.width(Math.max(supportedSwapSurfaceCapabilities.minImageExtent().width(),
											Math.min(supportedSwapSurfaceCapabilities.maxImageExtent().width(),600)));
			actuallExtent.width(Math.max(supportedSwapSurfaceCapabilities.minImageExtent().height(),
					Math.min(supportedSwapSurfaceCapabilities.maxImageExtent().height(),800)));
			
			return actuallExtent;
			
		}else {
			return supportedSwapSurfaceCapabilities.currentExtent();
		}
	}
	
	private VkSurfaceFormatKHR createSwapSurfaceFormat(int format, int colorSpace) {
		ByteBuffer buffer = memAlloc(VkSurfaceFormatKHR.SIZEOF);
		buffer.putInt(VkSurfaceFormatKHR.FORMAT,format);
		buffer.putInt(VkSurfaceFormatKHR.COLORSPACE,colorSpace);
		VkSurfaceFormatKHR ret = new VkSurfaceFormatKHR(buffer);
		memFree(buffer);
		return ret;
	}
	
	private int[] getSupportedPhysicalDevicePresentModes(VkPhysicalDevice physicalDevice, int[] supportedPhysicalDevicePresentModeCount) {
		vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.get(0),supportedPhysicalDevicePresentModeCount, null);
		int[] supportedPhysicalDevicePresentModes = new int[supportedPhysicalDevicePresentModeCount[0]];
		if(supportedPhysicalDevicePresentModeCount[0]>0) {
			vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.get(0),supportedPhysicalDevicePresentModeCount, supportedPhysicalDevicePresentModes);
		}
		return supportedPhysicalDevicePresentModes;
	}
	
	private VkSurfaceFormatKHR.Buffer getSupportedPhysicalDeviceFormats(VkPhysicalDevice physicalDevice, int[] supportedPhysicalDeviceFormatCount) {
		vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.get(0),supportedPhysicalDeviceFormatCount, null);
		VkSurfaceFormatKHR.Buffer supportedPhysicalDeviceFormats = VkSurfaceFormatKHR.calloc(supportedPhysicalDeviceFormatCount[0]);
		if(supportedPhysicalDeviceFormatCount[0]>0) {
			vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.get(0),supportedPhysicalDeviceFormatCount, supportedPhysicalDeviceFormats);
		}
		return supportedPhysicalDeviceFormats;
	}
	
	private VkSurfaceCapabilitiesKHR getSupportedSurfaceCapabilities(VkPhysicalDevice physicalDevice) {
		VkSurfaceCapabilitiesKHR supportedSurfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc();
		vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface.get(0),supportedSurfaceCapabilities);
		return supportedSurfaceCapabilities;
	}
	
	private void setupPresentThread() {
		presentThread = new PresentThread();
		presentThread.start();
	}
	
	private class framebufferResizeCallback implements GLFWFramebufferSizeCallbackI{
		
		@Override
		public void invoke(long window, int width, int height){
			framebufferResized = true;
		}
	}
	
	private class keyCallback implements GLFWKeyCallbackI{
		
		@Override
		public void invoke(long window, int key, int scancode, int action, int mods){
			
		}
	}
	
	public class PresentThread extends Thread{
		
		public final ArrayBlockingQueue<PresentationJob> jobQueue;
		public final Object waitObject = new Object();
		public volatile boolean running = true;
		public boolean waiting;
		
		public PresentThread() {
			jobQueue = new ArrayBlockingQueue<PresentationJob>(swapChainImages.limit());
		}
		
		@Override
		public void run() {
			while(running) {
				PresentationJob job;
				try {
					synchronized(this) {
						waiting = true;
					}
					synchronized(waitObject) {
						waitObject.notifyAll();
					}
					job = jobQueue.take();
				} catch (InterruptedException e) {
					continue;
				}
				synchronized(this) {
					waiting = false;
				}
				int err;
				synchronized(presentationQueue) {
					synchronized(swapChain) {
							err = vkQueuePresentKHR(presentationQueue, job.presentInfo);//Use VK_KHR_shared_presentable_image?
					}
				}
				if(err == VK_ERROR_OUT_OF_DATE_KHR || err == VK_SUBOPTIMAL_KHR || framebufferResized) {
					framebufferResized = false;
					setSwapChainMustBeRecreated();
				}else if(err != VK_SUCCESS) {
					throw new RuntimeException("Failed to present swap chain image: " + err);
				}
				synchronized(job.owner) {
				job.owner.imagePresented = true;
				}
				synchronized(waitAcquiredSwapChainImagesMonitorObject){
					imageAvailableSemaphoresList.add(job.imageAvailableSemaphore);
					currentAcquiredSwapChainImagesCount--;
					waitAcquiredSwapChainImagesMonitorObject.notifyAll();
				}
				
				memFree(job.presentInfo.pWaitSemaphores());
				memFree(job.presentInfo.pImageIndices());
				job.presentInfo.free();
			}
		}
		
		public void registerNewJob(VkPresentInfoKHR presentInfo, Long imageAvailableSemaphore, SceneRendererPerFrameThreadObject owner) {
			PresentationJob job = new PresentationJob(presentInfo, imageAvailableSemaphore, owner);
			try {
				jobQueue.put(job);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
		}
		
		private class PresentationJob{
			public final VkPresentInfoKHR presentInfo;
			public final Long imageAvailableSemaphore;
			public final SceneRendererPerFrameThreadObject owner;
			
			public PresentationJob(VkPresentInfoKHR presentInfo, Long imageAvailableSemaphore, SceneRendererPerFrameThreadObject owner) {
				this.presentInfo = presentInfo;
				this.imageAvailableSemaphore = imageAvailableSemaphore;
				this.owner = owner;
			}
		}
	}
	
	@Override
	protected void mainLoop() {
		while(!glfwWindowShouldClose(window)) {
			glfwPollEvents();
			drawFrame();
		}
		
		vkDeviceWaitIdle(device);//Wait for device finishing all Operations before cleaning up.
	}
	
	private void drawFrame() {
		if(!framebufferResized) {
			int[] imageIndex = new int[1];
			synchronized(waitAcquiredSwapChainImagesMonitorObject) {
				while(currentAcquiredSwapChainImagesCount > maxAcquiredSwapChainImagesCount) {
					try {
						waitAcquiredSwapChainImagesMonitorObject.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(-1);
					}
				}
			}
			Long nextSemaphore = null;
			synchronized(waitAcquiredSwapChainImagesMonitorObject){
				try {
					nextSemaphore = imageAvailableSemaphoresList.take();
				} catch (InterruptedException e) {
				}
			}
			int err = VK_NOT_READY;
			synchronized(swapChain){
					while(err == VK_NOT_READY) {
						err = vkAcquireNextImageKHR(device, swapChain.get(0), 100L, nextSemaphore.longValue(), VK_NULL_HANDLE, imageIndex);
					}
			}
			if(err == VK_ERROR_OUT_OF_DATE_KHR) {
				recreateSwapChain();
				return;
			}else if(err != VK_SUCCESS && err != VK_SUBOPTIMAL_KHR) {
				throw new RuntimeException("Failed to acquire swap chain image: " + err);
			}	
			
			synchronized(perFrameThreadObjects[imageIndex[0]].waitingMonitorObject) {
				boolean loop;
				synchronized(perFrameThreadObjects[imageIndex[0]]) {
					loop = !perFrameThreadObjects[imageIndex[0]].getWaiting();
				}
				while(loop) {
					try {
						perFrameThreadObjects[imageIndex[0]].waitingMonitorObject.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(-1);
					}
					synchronized(perFrameThreadObjects[imageIndex[0]]) {
						loop = !perFrameThreadObjects[imageIndex[0]].getWaiting();
					}
				}
			}
			//Thread waiting...
			
			perFrameThreadObjects[imageIndex[0]].setImageAvailableSemaphore(nextSemaphore);
			perFrameThreadObjects[imageIndex[0]].setImagePresented(false);
			
//!!!!!	FIXME: With the synchronizeblock no error occures		
			//synchronized(waitAcquiredSwapChainImagesMonitorObject) {
				currentAcquiredSwapChainImagesCount++;
			//}
				
//!!!!!
			perFrameThreadObjects[imageIndex[0]].setWaiting(false);
			if(perFrameThreadObjects[imageIndex[0]].isAlive()) {
				synchronized(perFrameThreadObjects[imageIndex[0]]) {
					perFrameThreadObjects[imageIndex[0]].notifyAll();
				}
			}else {
				perFrameThreadObjects[imageIndex[0]].start();
			}
			
		}else {
			framebufferResized = false;
			this.recreateSwapChain();
		}
		
		if(swapChainMustBeRecreated) {
			this.recreateSwapChain();
		}
	}
	
	protected void setSwapChainMustBeRecreated() {
			this.swapChainMustBeRecreated = true;
	}
	
	@Override
	protected void cleanup() {
		
		cleanupSwapChain();
		vkDestroySwapchainKHR(device, swapChain.get(0), null);

		for(int i=0;i<swapChainImages.limit();++i) {
			synchronized(perFrameThreadObjects[i].waitingMonitorObject) {
				boolean loop;
				synchronized(perFrameThreadObjects[i]) {
					loop = !perFrameThreadObjects[i].getWaiting();
				}
				while(loop) {
					try {
						perFrameThreadObjects[i].waitingMonitorObject.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(-1);
					}
					synchronized(perFrameThreadObjects[i]) {
						loop = !perFrameThreadObjects[i].getWaiting();
					}
				}
			}
			synchronized(perFrameThreadObjects[i]) {
				perFrameThreadObjects[i].setRunning(false);
				perFrameThreadObjects[i].setWaiting(false);
				perFrameThreadObjects[i].notify();
			}
			
		}
		
		presentThread.running = false;
		presentThread.interrupt();
		try {
			presentThread.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		for(int i=0;i<swapChainImages.limit();++i) {
			try {
				perFrameThreadObjects[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				//But dont break programm cause it is allready exiting
			}
			perFrameThreadObjects[i].cleanup();
			vkDestroySemaphore(device, imageAvailableSemaphores.get(i), null);
		}
		
		vkDestroyCommandPool(device, graphicsCommandPool.get(0), null);
		vkDestroyCommandPool(device, transferCommandPool.get(0), null);
		vkDestroyCommandPool(device, computeCommandPool.get(0), null);
		
		vkDestroyDevice(device, null);
		
		if(DEBUG_MODE) {
			vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger.get(0), (VkAllocationCallbacks)null);
		}
		
		vkDestroySurfaceKHR(instance, surface.get(0),null);
		
		vkDestroyInstance(instance, null);
		
		glfwDestroyWindow(window);
		
		glfwTerminate();
	}
	
	@Override
	protected void cleanupBuffers() {
		if(surface!=null) {
			memFree(surface);
		}
		if(debugMessenger!=null) {
			memFree(debugMessenger);
		}
		if(swapChain!=null) {
			memFree(swapChain);
		}
		if(swapChainImages!=null) {
			memFree(swapChainImages);
		}
		if(graphicsCommandPool!=null) {
			memFree(graphicsCommandPool);
		}
		if(transferCommandPool!=null) {
			memFree(transferCommandPool);
		}
		if(computeCommandPool!=null) {
			memFree(computeCommandPool);
		}
		if(requestedPhysicalDeviceProperties!=null) {
			requestedPhysicalDeviceProperties.free();
		}
		if(requestedPhysicalDeviceFeatures!=null) {
			requestedPhysicalDeviceFeatures.free();
		}
		if(swapChainExtent!=null) {
			swapChainExtent.free();
		}
		if(colorImages!=null) {
			memFree(colorImages);
		}
		if(colorImagesMemory!=null) {
			memFree(colorImagesMemory);
		}
		if(imageAvailableSemaphores!=null) {
			memFree(imageAvailableSemaphores);
		}
	}	
}
