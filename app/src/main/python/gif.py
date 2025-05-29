import os
from PIL import Image

def make_gif(files_dir, out_name="anim_imagenes.gif", duration=1):
    """
    Lee los PNG de Imagenes/t0…, los ordena y crea un GIF animado
    que sólo se reproduce una vez. Luego hacer click en Android
    recarga el GIF (en tu Activity) para volver a verlo.
    """
    imgs_dir = os.path.join(files_dir, "Imagenes")
    if not os.path.isdir(imgs_dir):
        raise FileNotFoundError(f"No existe el directorio de imágenes: {imgs_dir}")

    # 1) Carpeta tN ordenadas
    t_folders = sorted(
        [d for d in os.listdir(imgs_dir) if d.startswith("t")],
        key=lambda x: int(x[1:])
    )

    # 2) Abrimos cada image.png con Pillow
    frames = []
    for tname in t_folders:
        img_path = os.path.join(imgs_dir, tname, "image.png")
        if os.path.isfile(img_path):
            frames.append(Image.open(img_path).convert("RGBA"))
    if not frames:
        raise FileNotFoundError("No hay imágenes en Imagenes/ para crear el GIF.")

    # 3) Guardamos el GIF con loop=1 (se reproduce sólo una vez)
    gif_path = os.path.join(files_dir, out_name)
    # duration en ms
    ms = int(duration * 1000)
    frames[0].save(
        gif_path,
        save_all=True,
        append_images=frames[1:],
        duration=ms,
        loop=1,
        disposal=2
    )
    return gif_path