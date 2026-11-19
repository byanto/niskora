interface ImportMetaEnv {
  /** The commit the image was built from (Dockerfile); unset in a local build. */
  readonly VITE_BUILD_ID?: string;
}
