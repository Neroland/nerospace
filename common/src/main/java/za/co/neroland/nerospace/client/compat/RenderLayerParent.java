package za.co.neroland.nerospace.client.compat;

// Minecraft 1.21.1 only (empty on every other line): what a client.compat EyesLayer draws from - the state-posed model
// of a client.compat MobRenderer (vanilla's 1.21.1 RenderLayerParent is typed on the entity, not a render state).

//? if <1.21.2 {
/*public interface RenderLayerParent<S extends LivingEntityRenderState, M extends EntityModel<S>> {

    M stateModel();
}
*///?}
