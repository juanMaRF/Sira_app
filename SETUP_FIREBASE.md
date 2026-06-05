# Conectar Sira con Firebase

La app ya funciona en **modo demostración** (datos simulados). Sigue estos pasos
para conectar tu cuenta real de Firebase. No necesitas cambiar la UI ni los
ViewModels: solo configuración y un archivo (`di/ServiceLocator.kt`).

## 1. Crear el proyecto en la consola de Firebase
1. Entra a https://console.firebase.google.com y haz clic en **Agregar proyecto**.
2. Ponle un nombre (p. ej. `Sira`), acepta los términos y crea el proyecto.
   (Google Analytics es opcional para este proyecto).

## 2. Registrar la app de Android
1. En el panel del proyecto, haz clic en el ícono de **Android** (Agregar app).
2. En **Nombre del paquete de Android** escribe exactamente:
   ```
   com.example.sira
   ```
3. (Recomendado) Agrega la **huella SHA-1**, obligatoria para que Google Sign-In
   funcione. Obtenla con:
   ```powershell
   .\gradlew.bat signingReport
   ```
   Copia el valor `SHA1` de la variante `debug` y pégalo en Firebase
   (Configuración del proyecto → Tus apps → Agregar huella digital).

## 3. Descargar google-services.json
1. Descarga el archivo **`google-services.json`** que te ofrece la consola.
2. Cópialo dentro de la carpeta **`app/`** del proyecto:
   ```
   app/google-services.json
   ```

## 4. Activar el plugin de Google Services
1. En `app/build.gradle.kts`, **descomenta** esta línea dentro de `plugins { }`:
   ```kotlin
   alias(libs.plugins.google.services)
   ```
2. Sincroniza Gradle (Sync Now).

## 5. Habilitar los métodos de inicio de sesión
1. En la consola: **Compilación → Authentication → Comenzar**.
2. Pestaña **Sign-in method**.
3. Habilita **Correo electrónico/contraseña** → Guardar.
   (Esto permite crear cuenta e iniciar sesión sin Google.)
4. Habilita también **Google** → Guardar.
5. Copia el **Web client ID** (ID de cliente web) que Firebase genera
   (Authentication → Sign-in method → Google → Configuración del SDK web, o en
   Configuración del proyecto). Tiene forma:
   ```
   1234567890-xxxxxxxx.apps.googleusercontent.com
   ```

## 6. Crear la base de datos (Firestore)
1. En la consola: **Compilación → Firestore Database → Crear base de datos**.
2. Elige **modo de prueba** (para desarrollo) y una ubicación cercana.
3. **No necesitas crear documentos a mano**: la app los crea/reclama al registrar
   una planta. Aun así, esta es la estructura que maneja:

   ```
   plants (colección)
     └── SIRA-7F3A   (documento; su ID es el código de la maceta)
           ├── ownerUid:    "abc123"      ← UID del usuario dueño (lo escribe la app)
           ├── plantName:    "Albahaca"
           ├── soilMoisture: 62           ┐
           ├── temperature:  23.5         │  los escribe la ESP32
           ├── lightLevel:   74           │  (alimentan el Panel)
           ├── waterLevel:   88           │
           ├── lastUpdated:  1717000000000┘
           └── history (subcolección)     ← una lectura por documento (Histórico)
   ```

   - El **Panel** lee el documento `plants/{código}`.
   - El **Histórico** lee `plants/{código}/history`, las últimas 50 lecturas
     ordenadas por `lastUpdated`. Cuando la maceta mida, debe: (a) actualizar el
     documento principal y (b) agregar un documento nuevo a `history`.

## 7. Registro de plantas (cómo funciona en la app)
El usuario, tras iniciar sesión, ve **"Mis plantas"**. Para agregar una, escribe
el **código** de la maceta (= ID del documento) y un nombre. La app, en modo
**híbrido**:
- si `plants/{código}` existe y no tiene `ownerUid` → lo **reclama** para el usuario;
- si ya es del usuario → lo deja igual;
- si es de otra cuenta → muestra error;
- si no existe → lo **crea** con ese código y `ownerUid`.

La app solo muestra las plantas donde `ownerUid == miUid`.

## 8. Reglas de seguridad (IMPORTANTE)
Por defecto el "modo de prueba" deja la base abierta y caduca a los 30 días.
Reemplaza las reglas (Firestore → Reglas) por estas, que garantizan que cada
usuario solo acceda a SUS plantas:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /plants/{plantId} {
      // Leer/editar solo plantas propias.
      allow read, write: if request.auth != null
                         && resource.data.ownerUid == request.auth.uid;
      // Reclamar una maceta sin dueño, o crearla a tu nombre.
      allow create: if request.auth != null
                    && request.resource.data.ownerUid == request.auth.uid;
      allow update: if request.auth != null
                    && (resource.data.ownerUid == null
                        || resource.data.ownerUid == request.auth.uid)
                    && request.resource.data.ownerUid == request.auth.uid;

      // El histórico hereda la propiedad de la planta.
      match /history/{readingId} {
        allow read, write: if request.auth != null
          && get(/databases/$(database)/documents/plants/$(plantId)).data.ownerUid == request.auth.uid;
      }
    }
  }
}
```

> Nota: la ESP32 escribe con sus propias credenciales (cuenta de servicio o
> token), no con estas reglas de usuario. Si por ahora la maceta escribe sin
> autenticación, mantén el modo de prueba mientras desarrollas.

## 9. Probar la UI sin datos reales
En `di/ServiceLocator.kt` puedes volver temporalmente a las versiones Mock
(`MockPlantRepository()`, `MockPlantsRepository()`, `MockAuthRepository()`) para
recorrer todo el flujo (login → registrar → panel → histórico) sin tocar Firebase.
