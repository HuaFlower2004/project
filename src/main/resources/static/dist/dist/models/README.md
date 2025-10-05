# 3D模型文件说明

这个目录用于存放登录页面的3D模型文件。

## 支持的文件格式

- **GLTF/GLB** (.gltf, .glb) - 推荐格式，支持动画和材质
- **DAE** (.dae) - Collada格式
- **USDZ** (.usdz) - Apple的通用场景描述格式
- **OBJ** (.obj) - 简单的几何体格式
- **FBX** (.fbx) - Autodesk格式

## 使用方法

1. 将你的3D模型文件放在 `public/models/` 目录下
2. 在 `src/components/Login3DModel.vue` 中修改 `load3DModel()` 函数
3. 取消注释相关代码并指定正确的文件路径

## 示例代码

```javascript
function load3DModel() {
  const loader = new GLTFLoader();
  const dracoLoader = new DRACOLoader();
  dracoLoader.setDecoderPath('/draco/');
  loader.setDRACOLoader(dracoLoader);
  
  loader.load(
    '/models/your-model.gltf', // 替换为你的模型文件路径
    (gltf) => {
      if (model) {
        scene.remove(model);
      }
      model = gltf.scene;
      model.scale.set(1, 1, 1);
      model.position.set(0, 0, 0);
      scene.add(model);
      
      // 设置阴影
      model.traverse((child) => {
        if (child.isMesh) {
          child.castShadow = true;
          child.receiveShadow = true;
        }
      });
      
      // 如果有动画
      if (gltf.animations && gltf.animations.length) {
        mixer = new THREE.AnimationMixer(model);
        const action = mixer.clipAction(gltf.animations[0]);
        action.play();
      }
    },
    (progress) => {
      console.log('Loading progress:', (progress.loaded / progress.total * 100) + '%');
    },
    (error) => {
      console.error('Error loading model:', error);
    }
  );
}
```

## 推荐的免费3D模型资源

- **Sketchfab** - 大量免费和付费模型
- **TurboSquid** - 专业3D模型市场
- **CGTrader** - 3D模型交易平台
- **BlendSwap** - Blender用户分享平台
- **Free3D** - 免费3D模型网站

## 模型优化建议

1. **文件大小** - 建议模型文件小于5MB
2. **多边形数量** - 建议少于10,000个面
3. **纹理分辨率** - 建议纹理不超过2048x2048
4. **格式选择** - 优先使用GLTF/GLB格式
5. **压缩** - 使用Draco压缩减少文件大小

## 注意事项

- 确保模型文件有适当的许可证
- 测试模型在不同设备上的性能
- 考虑添加加载进度提示
- 提供模型加载失败的后备方案 