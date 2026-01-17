import os

def merge_project_files(target_dir, output_file, custom_excludes=None):
    """
    将代码、配置文件和样式表合并，支持自定义排除目录
    """
    # 默认包含的后缀名
    extensions = {
        '.py', '.java', '.cpp', '.c', '.h', '.go', '.rs', '.js', '.ts', '.php',
        '.json', '.yaml', '.yml', '.toml', '.xml', '.conf', '.ini', '.env',
        '.css', '.scss', '.sass', '.less', '.html', '.md'
    }

    # 基础排除目录（通常不需要的代码库或工具目录）
    exclude_dirs = {'.git', 'node_modules', '__pycache__', '.venv', 'dist', 'build'}
    
    # 合并用户自定义的排除目录
    if custom_excludes:
        exclude_dirs.update(custom_excludes)

    print(f"正在扫描目录: {target_dir}")
    print(f"将排除以下目录: {exclude_dirs}")

    with open(output_file, 'w', encoding='utf-8') as outfile:
        count = 0
        for root, dirs, files in os.walk(target_dir):
            # 过滤目录：修改 dirs 列表会影响 os.walk 的后续遍历
            dirs[:] = [d for d in dirs if d not in exclude_dirs]

            for file in files:
                ext = os.path.splitext(file)[1].lower()
                if ext in extensions:
                    file_path = os.path.join(root, file)
                    relative_path = os.path.relpath(file_path, target_dir)
                    
                    # 写入格式化的头部
                    outfile.write(f"\n{'='*80}\n")
                    outfile.write(f"PATH: {relative_path}\n")
                    outfile.write(f"{'='*80}\n\n")
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as infile:
                            outfile.write(infile.read())
                            outfile.write("\n")
                        count += 1
                    except Exception as e:
                        outfile.write(f"// ERROR: 无法读取文件 {relative_path} - {e}\n")

    print(f"处理完成！共合并 {count} 个文件至: {output_file}")

if __name__ == "__main__":
    # --- 用户配置区 ---
    # 1. 你的项目根目录
    project_root = "./" 
    
    # 2. 输出文件名
    output_path = "test.txt"
    
    # 3. 在这里指定你想要额外排除的文件夹名称
    # 例如：'images', 'tests', 'logs'
    my_excludes = {"libs",'app'} 
    
    # 运行
    merge_project_files(project_root, output_path, my_excludes)