import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.EXTBlendOperationAdvanced.VK_ACCESS_COLOR_ATTACHMENT_READ_NONCOHERENT_BIT_EXT;
import static org.lwjgl.vulkan.EXTBlendOperationAdvanced.VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTConditionalRendering.VK_STRUCTURE_TYPE_CONDITIONAL_RENDERING_BEGIN_INFO_EXT;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.EXTTransformFeedback.VK_BUFFER_USAGE_TRANSFORM_FEEDBACK_BUFFER_BIT_EXT;
import static org.lwjgl.vulkan.EXTTransformFeedback.VK_BUFFER_USAGE_TRANSFORM_FEEDBACK_COUNTER_BUFFER_BIT_EXT;
import static org.lwjgl.vulkan.VK11.VK_IMAGE_LAYOUT_DEPTH_READ_ONLY_STENCIL_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.EXTConditionalRendering.vkCmdBeginConditionalRenderingEXT;
import static org.lwjgl.vulkan.EXTConditionalRendering.vkCmdEndConditionalRenderingEXT;
import static org.lwjgl.vulkan.EXTShaderViewportIndexLayer.VK_EXT_SHADER_VIEWPORT_INDEX_LAYER_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTTransformFeedback.vkCmdBindTransformFeedbackBuffersEXT;
import static org.lwjgl.vulkan.EXTTransformFeedback.vkCmdBeginTransformFeedbackEXT;
import static org.lwjgl.vulkan.EXTTransformFeedback.vkCmdEndTransformFeedbackEXT;
import static org.lwjgl.vulkan.EXTTransformFeedback.vkCmdDrawIndirectByteCountEXT;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.LinkedList;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkClearAttachment;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkClearDepthStencilValue;
import org.lwjgl.vulkan.VkClearRect;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkConditionalRenderingBeginInfoEXT;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageCopy;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageResolve;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkMemoryBarrier;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkOffset3D;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkViewport;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class SceneRendererPerFrameThreadObject extends VulkanApplicationPerFrameThreadObject<SceneRenderer>{

	public static final boolean ENABLE_DEFAULT_CAMERA = true;
	
	private boolean running = false;
	private boolean waiting = true;
	public final Object waitingMonitorObject = new Object();
	
	private final LongBuffer swapChainImageView = memAllocLong(1);
	private final LongBuffer colorImageView = memAllocLong(1);
	private long swapChainImage;
	private long colorImage;
	private final LongBuffer renderFinishedSemaphores;
	private final LongBuffer inFlightFences;
	private final int imageIndex;
	private Long imageAvailableSemaphore;
	protected boolean imagePresented;
	
	private int currentCommandBufferIndex = 0;
	
	public SceneRendererPerFrameThreadObject(SceneRenderer parentProcess, long swapChainImage, long colorImage, int imageIndex) {
		super(parentProcess);
		synchronized(this) {
			this.swapChainImage = swapChainImage;
			this.colorImage = colorImage;
			this.imageIndex = imageIndex;
			
			createCommandPools();
			createImageViews();
			long[] commanBufferPointers = allocateCommandBuffers();
			commandBuffers = new VkCommandBuffer[commanBufferPointers.length];
			for(int i=0;i<commanBufferPointers.length;++i) {
				commandBuffers[i] = new VkCommandBuffer(commanBufferPointers[i], parentProcess.device);
			}
			inFlightFences = memAllocLong(commandBuffers.length);
			renderFinishedSemaphores = memAllocLong(commandBuffers.length);
			createSyncObjects();
			recordCommandBuffer();
		}
	}
	
	private void createImageViews() {
		long currentView = VK_NULL_HANDLE;
		
		//currentView = parentProcess.createImageView(swapChainImage, parentProcess.swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1, 0, 1, VK_IMAGE_VIEW_TYPE_2D);
		currentView = VK_NULL_HANDLE;
		swapChainImageView.put(0, currentView);
		
		currentView = parentProcess.createImageView(colorImage, parentProcess.colorImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1, 0, 1, VK_IMAGE_VIEW_TYPE_2D);
		colorImageView.put(0, currentView);
	}
	
	protected void recreateSwapChain(long newSwapChainImage, long newColorImage) {
		this.swapChainImage = newSwapChainImage;
		this.colorImage = newColorImage;
		
		createImageViews();
		
		allocateCommandBuffers();
		recordCommandBuffer();
	}
	
	protected void cleanupSwapChain() {
		vkWaitForFences(parentProcess.device, inFlightFences, true, -1L);
		
		if(swapChainImageView.get(0) != VK_NULL_HANDLE) {
			vkDestroyImageView(parentProcess.device, swapChainImageView.get(0), null);
		}
		vkDestroyImageView(parentProcess.device, colorImageView.get(0), null);
	}
	
	@Override
	protected void recordCommandBuffer() {//TODO: Use secondary command buffers.
		VkCommandBuffer commandBuffer = commandBuffers[currentCommandBufferIndex];
		
		VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc();
		commandBufferBeginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
		commandBufferBeginInfo.flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);
		commandBufferBeginInfo.pInheritanceInfo(null);
		
		int err = vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo);
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to begin recording CommandBuffer: " + err);
		}
		
		//Copy Color Output to SwapChain
		VkImageSubresourceRange colorSwapCopySubresourceRange = VkImageSubresourceRange.calloc();
		colorSwapCopySubresourceRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
		colorSwapCopySubresourceRange.baseMipLevel(0);
		colorSwapCopySubresourceRange.levelCount(1);
		colorSwapCopySubresourceRange.baseArrayLayer(0);
		colorSwapCopySubresourceRange.layerCount(1);
		
		VkImageMemoryBarrier.Buffer colorSwapCopyColorMemoryBarrier = VkImageMemoryBarrier.calloc(1);
		colorSwapCopyColorMemoryBarrier.get(0).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
		colorSwapCopyColorMemoryBarrier.get(0).srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT |
				VK_ACCESS_COLOR_ATTACHMENT_READ_BIT |
				VK_ACCESS_COLOR_ATTACHMENT_READ_NONCOHERENT_BIT_EXT);
		colorSwapCopyColorMemoryBarrier.get(0).dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
		colorSwapCopyColorMemoryBarrier.get(0).oldLayout(VK_IMAGE_LAYOUT_GENERAL);
		colorSwapCopyColorMemoryBarrier.get(0).newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
		colorSwapCopyColorMemoryBarrier.get(0).srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		colorSwapCopyColorMemoryBarrier.get(0).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		colorSwapCopyColorMemoryBarrier.get(0).image(colorImage);
		colorSwapCopyColorMemoryBarrier.get(0).subresourceRange(colorSwapCopySubresourceRange);
		
		VkImageMemoryBarrier.Buffer colorSwapCopySwapMemoryBarrier = VkImageMemoryBarrier.calloc(1);
		colorSwapCopySwapMemoryBarrier.get(0).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
		colorSwapCopySwapMemoryBarrier.get(0).srcAccessMask(0);
		colorSwapCopySwapMemoryBarrier.get(0).dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
		colorSwapCopySwapMemoryBarrier.get(0).oldLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
		colorSwapCopySwapMemoryBarrier.get(0).newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
		colorSwapCopySwapMemoryBarrier.get(0).srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		colorSwapCopySwapMemoryBarrier.get(0).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		colorSwapCopySwapMemoryBarrier.get(0).image(swapChainImage);
		colorSwapCopySwapMemoryBarrier.get(0).subresourceRange(colorSwapCopySubresourceRange);
		
		vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_DEPENDENCY_BY_REGION_BIT, null, null, colorSwapCopyColorMemoryBarrier);
		vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_DEPENDENCY_BY_REGION_BIT, null, null, colorSwapCopySwapMemoryBarrier);
		
		VkImageSubresourceLayers colorSwapCopyLayers = VkImageSubresourceLayers.calloc();
		colorSwapCopyLayers.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
		colorSwapCopyLayers.mipLevel(0);
		colorSwapCopyLayers.baseArrayLayer(0);
		colorSwapCopyLayers.layerCount(1);
		
		VkOffset3D colorSwapCopyOffset = VkOffset3D.calloc();
		colorSwapCopyOffset
		.x(0)
		.y(0)
		.z(0);
		
		VkExtent3D colorSwapCopyExtent = VkExtent3D.calloc();
		colorSwapCopyExtent
		.width(parentProcess.swapChainExtent.width())
		.height(parentProcess.swapChainExtent.height())
		.depth(1);
		
		
		VkImageCopy.Buffer colorSwapCopyRegions = VkImageCopy.calloc(1);
		colorSwapCopyRegions.srcSubresource(colorSwapCopyLayers);
		colorSwapCopyRegions.srcOffset(colorSwapCopyOffset);
		colorSwapCopyRegions.dstSubresource(colorSwapCopyLayers);
		colorSwapCopyRegions.dstOffset(colorSwapCopyOffset);
		colorSwapCopyRegions.extent(colorSwapCopyExtent);
		
		vkCmdCopyImage(commandBuffer, colorImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, swapChainImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, colorSwapCopyRegions);
		colorSwapCopyRegions.free();
		
		colorSwapCopyColorMemoryBarrier.get(0).srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
		colorSwapCopyColorMemoryBarrier.get(0).dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT |
				VK_ACCESS_COLOR_ATTACHMENT_READ_BIT |
				VK_ACCESS_COLOR_ATTACHMENT_READ_NONCOHERENT_BIT_EXT);
		colorSwapCopyColorMemoryBarrier.get(0).oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
		colorSwapCopyColorMemoryBarrier.get(0).newLayout(VK_IMAGE_LAYOUT_GENERAL);
		
		colorSwapCopySwapMemoryBarrier.get(0).srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
		colorSwapCopySwapMemoryBarrier.get(0).dstAccessMask(0);
		colorSwapCopySwapMemoryBarrier.get(0).oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
		colorSwapCopySwapMemoryBarrier.get(0).newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
		
		
		vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_DEPENDENCY_BY_REGION_BIT, null, null, colorSwapCopyColorMemoryBarrier);
		vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, VK_DEPENDENCY_BY_REGION_BIT, null, null, colorSwapCopySwapMemoryBarrier);
		
		err = vkEndCommandBuffer(commandBuffer);
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to record CommandBuffer: " + err);
		}
		
		commandBufferBeginInfo.free();
		colorSwapCopyLayers.free();
		colorSwapCopyOffset.free();
		colorSwapCopyExtent.free();
		colorSwapCopySwapMemoryBarrier.free();
		colorSwapCopyColorMemoryBarrier.free();
		colorSwapCopySubresourceRange.free();
	}
	
	private void createSyncObjects() {
		VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc();
		semaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
		
		VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc();
		fenceCreateInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
		fenceCreateInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);//Submit Fences Signaled
		
		LongBuffer currentSyncObject = memAllocLong(1);
		int err;
		for(int i=0;i<renderFinishedSemaphores.limit();++i) {
			err = vkCreateSemaphore(parentProcess.device, semaphoreCreateInfo, null, currentSyncObject);
			if (err != VK_SUCCESS) {
				throw new AssertionError("Failed to create Semaphores: " + err);
			}
			
			renderFinishedSemaphores.put(i, currentSyncObject.get(0));
		}
		
		for(int i=0;i<inFlightFences.limit();++i) {
			err = vkCreateFence(parentProcess.device, fenceCreateInfo, null, currentSyncObject);
			if (err != VK_SUCCESS) {
				throw new AssertionError("Failed to create Fences: " + err);
			}
			
			inFlightFences.put(i, currentSyncObject.get(0));
		}

		semaphoreCreateInfo.free();
		fenceCreateInfo.free();
		memFree(currentSyncObject);
	}
	
	public void setImageAvailableSemaphore(Long imageAvailableSemaphore) {
		this.imageAvailableSemaphore = imageAvailableSemaphore;
	}
	
	public void setImagePresented(boolean imagePresented) {
		this.imagePresented = imagePresented;
	}
	
	public boolean getImagePresented() {
		return imagePresented;
	}
	
	public void setRunning(boolean running) {
		synchronized(this) {
			this.running = running;
		}
	}
	
	public boolean getRunning() {
		synchronized(this) {
			return running;
		}
	}
	
	public void setWaiting(boolean waiting) {
		synchronized(this) {
			this.waiting = waiting;
		}
	}
	
	public boolean getWaiting() {
		synchronized(this) {
			return waiting;
		}
	}
	
	@Override
	public void run() {
		synchronized(this){
			running = true;
		}
		boolean currRunning;
		synchronized(this) {
			currRunning = running;
		}
		while(currRunning) {
			drawFrame();
			currentCommandBufferIndex = (currentCommandBufferIndex+1)%commandBuffers.length;
			updateCommandBuffer();
			synchronized(this) {
				waiting = true;
			}
			synchronized(waitingMonitorObject){
				waitingMonitorObject.notifyAll();
			}
			synchronized(this) {
				while(waiting) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(-1);
					}
				}
			}
			synchronized(this) {
				currRunning = running;
			}
		}
	}
	
	private void drawFrame() {
		vkWaitForFences(parentProcess.device, inFlightFences.get(currentCommandBufferIndex), true, -1L);
		
		VkSubmitInfo.Buffer submitInfos = VkSubmitInfo.calloc(1);
		submitInfos.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
		submitInfos.waitSemaphoreCount(1);
		LongBuffer waitSemaphores = (LongBuffer) memAllocLong(1).put(imageAvailableSemaphore.longValue()).flip();
		submitInfos.pWaitSemaphores(waitSemaphores);
		IntBuffer waitStages = memAllocInt(1);
		//waitStages.put(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).flip();
		waitStages.put(VK_PIPELINE_STAGE_TRANSFER_BIT).flip();
		submitInfos.pWaitDstStageMask(waitStages);//Wait with writing of output image till it's available
		PointerBuffer commandBufferPointer = memAllocPointer(1).put(commandBuffers[currentCommandBufferIndex].address()).flip();
		submitInfos.pCommandBuffers(commandBufferPointer);
		LongBuffer signalSemaphores = (LongBuffer) memAllocLong(1).put(renderFinishedSemaphores.get(currentCommandBufferIndex)).flip();
		submitInfos.pSignalSemaphores(signalSemaphores);//After executing signal semaphore
		
		vkResetFences(parentProcess.device, inFlightFences.get(currentCommandBufferIndex));

		int err = -1;
		synchronized(parentProcess.graphicsQueue) {
			err = vkQueueSubmit(parentProcess.graphicsQueue, submitInfos, inFlightFences.get(currentCommandBufferIndex));//TODO: Only one call when rendering Screen for all Frames
		}
		if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit draw CommandBuffer: " + err);
		}
		
		VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc();
		presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
		presentInfo.pWaitSemaphores(signalSemaphores);//wait with presentation till Image is rendered
		presentInfo.swapchainCount(1);
		presentInfo.pSwapchains(parentProcess.swapChain);
		IntBuffer imageIndexBuffer = (IntBuffer) memAllocInt(1).put(imageIndex).flip();
		presentInfo.pImageIndices(imageIndexBuffer);
		presentInfo.pResults(null);
		
		parentProcess.presentThread.registerNewJob(presentInfo, imageAvailableSemaphore, this);
		
		memFree(commandBufferPointer);
		memFree(waitStages);
		memFree(waitSemaphores);
		submitInfos.free();
	}
	
	private void updateCommandBuffer() {
		vkWaitForFences(parentProcess.device, inFlightFences.get(currentCommandBufferIndex), true, -1L);
		vkResetCommandPool(parentProcess.device, graphicsCommandPools.get(currentCommandBufferIndex), 0);
		vkResetCommandPool(parentProcess.device, transferCommandPools.get(0), 0);
		recordCommandBuffer();
	}
	
	@Override
	protected void cleanup() {
		for(int i=0;i<graphicsCommandPools.limit();++i) {
			vkDestroyCommandPool(parentProcess.device, graphicsCommandPools.get(i), null);
		}
		for(int i=0;i<transferCommandPools.limit();++i) {
			vkDestroyCommandPool(parentProcess.device, transferCommandPools.get(i), null);
		}
		for(int i=0;i<renderFinishedSemaphores.limit();++i) {
			vkDestroySemaphore(parentProcess.device, renderFinishedSemaphores.get(i), null);
		}
		for(int i=0;i<inFlightFences.limit();++i) {
			vkDestroyFence(parentProcess.device, inFlightFences.get(i), null);
		}
		
		cleanupBuffers();
	}
	
	@Override
	protected void cleanupBuffers() {
		if(graphicsCommandPools!=null) {
			memFree(graphicsCommandPools);
		}
		if(transferCommandPools!=null) {
			memFree(transferCommandPools);
		}
		if(swapChainImageView!=null) {
			memFree(swapChainImageView);
		}
		if(renderFinishedSemaphores!=null) {
			memFree(renderFinishedSemaphores);
		}
		if(inFlightFences!=null) {
			memFree(inFlightFences);
		}
		if(colorImageView!=null) {
			memFree(colorImageView);
		}
	}
}
