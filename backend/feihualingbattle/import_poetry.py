#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
诗词数据批量导入脚本
功能：从 chinese-poetry-data 目录读取 JSON 文件，繁简转换后批量导入 MySQL 数据库
依赖：pip install pymysql opencc-python-reimplemented json
"""

import os
import sys
import json
import time
import logging
import traceback
import re
import pymysql
from datetime import datetime
from collections import Counter

try:
    import opencc
    CONVERTER = opencc.OpenCC('t2s')  # 繁体转简体
except ImportError:
    print("⚠️  opencc 库未安装，将跳过繁简转换")
    print("   安装命令: pip install opencc-python-reimplemented")
    CONVERTER = None

# ==================== 日志配置 ====================
# 配置日志记录器
logger = logging.getLogger('poetry_import')
logger.setLevel(logging.DEBUG)

# 创建文件处理器（详细日志，带时间戳）
log_filename = f'import_log_{datetime.now().strftime("%Y%m%d_%H%M%S")}.txt'
file_handler = logging.FileHandler(log_filename, encoding='utf-8')
file_handler.setLevel(logging.DEBUG)
file_formatter = logging.Formatter('%(asctime)s [%(levelname)s] %(message)s')
file_handler.setFormatter(file_formatter)
logger.addHandler(file_handler)

# 创建控制台处理器
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.INFO)
console_formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
console_handler.setFormatter(console_formatter)
logger.addHandler(console_handler)

logger.info(f"日志文件已创建: {log_filename}")

# ==================== 配置区域 ====================
DB_CONFIG = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'port': int(os.environ.get('DB_PORT', '3306')),
    'user': os.environ.get('DB_USERNAME', 'root'),
    'password': os.environ.get('DB_PASSWORD', 'your_db_password'),
    'database': os.environ.get('DB_NAME', 'feihualingbattle'),
    'charset': 'utf8mb4'
}

# 获取脚本所在目录作为基准路径
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# 数据目录路径（使用相对路径）
TANG_DIR = os.path.join(BASE_DIR, 'chinese-poetry-data', 'tang')
SONG_DIR = os.path.join(BASE_DIR, 'chinese-poetry-data', 'song')

# ==================== 全局统计 ====================
# 记录繁简转换异常统计
conversion_stats = {
    'total_conversions': 0,
    'length_changed': 0,
    'lost_characters': Counter(),
    'failed_conversions': 0
}

# 数据质量统计
data_quality_stats = {
    'empty_titles': 0,              # 空标题
    'empty_authors': 0,             # 空作者
    'empty_paragraphs': 0,          # 空段落（失传）
    'invalid_data_types': 0,        # 数据类型错误
    'duplicate_poems': 0,           # 重复诗词
    'unusual_punctuation': 0,       # 异常标点
    'very_long_lines': 0,           # 超长句子
    'very_short_lines': 0,          # 超短句子
    'missing_dynasty': 0,           # 缺失朝代
    'encoding_issues': 0,           # 编码问题
    'special_characters': Counter() # 特殊字符统计
}


def convert_to_simplified(text):
    """繁体转简体"""
    if not CONVERTER or not text:
        return text
    
    conversion_stats['total_conversions'] += 1
    
    try:
        simplified = CONVERTER.convert(text)
        # 验证转换后是否有字符丢失（生僻字可能无法转换）
        if len(simplified) != len(text):
            conversion_stats['length_changed'] += 1
            logger.warning(f"⚠️ 繁简转换后字符长度变化 - 原文({len(text)}字): {text[:50]}, 转换后({len(simplified)}字): {simplified[:50]}")
            # 找出丢失的字符
            original_chars = set(text)
            simplified_chars = set(simplified)
            lost_chars = original_chars - simplified_chars
            if lost_chars:
                for char in lost_chars:
                    conversion_stats['lost_characters'][char] += 1
                logger.warning(f"   丢失的字符: {lost_chars}")
        return simplified
    except Exception as e:
        conversion_stats['failed_conversions'] += 1
        logger.error(f"❌ 繁简转换异常: {e}, 文本: {text[:100]}")
        return text


def calculate_total_chars(paragraphs):
    """计算总字符数（不含标点）"""
    total = 0
    for p in paragraphs:
        total += len(p.replace('，', '').replace('。', '').replace('！', '')
                     .replace('？', '').replace('；', '').replace('、', '').replace(' ', ''))
    return total


def determine_poetry_type(dynasty):
    """判断诗词类型"""
    if dynasty == '唐':
        return '唐诗'
    elif dynasty == '宋':
        return '宋词'
    else:
        return '其他'


def split_poetry_lines(paragraphs):
    """将诗词拆分为半句列表"""
    lines = []
    for para in paragraphs:
        # 按标点拆分
        parts = []
        current = ''
        for char in para:
            if char in '，。！？；、':
                if current.strip():
                    parts.append(current.strip())
                current = ''
            else:
                current += char
        if current.strip():
            parts.append(current.strip())
        lines.extend(parts)
    return lines


def build_poem_structure(paragraphs):
    """构建诗词结构JSON数组"""
    structure = []
    line_order = 1
    
    for para in paragraphs:
        # 按标点拆分
        current_line = ''
        for char in para:
            if char in '，。！？；、':
                if current_line.strip():
                    structure.append({
                        "line": current_line.strip(),
                        "order": line_order
                    })
                    line_order += 1
                current_line = ''
            else:
                current_line += char
        
        if current_line.strip():
            structure.append({
                "line": current_line.strip(),
                "order": line_order
            })
            line_order += 1
    
    return structure


def validate_poetry_content(text, field_name, title=''):
    """验证诗词内容的质量（仅统计，不记录日志）"""
    if not text:
        return
    
    # 检查异常标点（连续标点）
    if re.search(r'[，。！？；、]{2,}', text):
        data_quality_stats['unusual_punctuation'] += 1
    
    # 检查特殊字符（非中文、非标点、非空格）
    special_chars = []
    for char in text:
        # 中文字符范围：\u4e00-\u9fff，扩展A区：\u3400-\u4dbf
        # 标点符号：，。！？；、
        if not ('\u4e00' <= char <= '\u9fff' or 
                '\u3400' <= char <= '\u4dbf' or
                char in '，。！？；、 ' or
                char.isascii() and char.isalnum()):
            special_chars.append(char)
    
    if special_chars:
        for char in set(special_chars):
            data_quality_stats['special_characters'][char] += 1
    
    # 检查句子长度
    sentences = re.split(r'[。！？]', text)
    for sentence in sentences:
        sentence = sentence.strip('，；、')
        if len(sentence) > 50:
            data_quality_stats['very_long_lines'] += 1
        elif len(sentence) > 0 and len(sentence) < 2:
            data_quality_stats['very_short_lines'] += 1
    
    # 注意：不再记录警告日志，数据会正常导入


def insert_poetry_data(cursor, poem_data):
    """插入单首诗词数据到数据库"""
    try:
        if not isinstance(poem_data, dict):
            data_quality_stats['invalid_data_types'] += 1
            logger.warning(f"❌ 无效的数据格式: {type(poem_data)}, 值: {repr(poem_data)[:100]}")
            return False
            
        title = poem_data.get('title', '')
        author = poem_data.get('author', '')
        dynasty = poem_data.get('dynasty', '')
        paragraphs = poem_data.get('paragraphs', [])
        
        # 验证必填字段
        if not title:
            # 标题为空时，自动填充为"标题待考证"，允许正常导入
            title = "标题待考证"
            data_quality_stats['empty_titles'] += 1
        
        if not author:
            data_quality_stats['empty_authors'] += 1
            logger.warning(f"⚠️ 作者为空 - 标题: '{title}'")
        
        # 注意：朝代可以从文件路径推断，所以不强制要求
        if not dynasty:
            # 不再记录为错误，因为会在 process_json_file 中自动填充
            pass
        
        # 验证数据类型
        if not isinstance(title, str) or not isinstance(author, str):
            data_quality_stats['invalid_data_types'] += 1
            logger.warning(f"❌ 数据类型错误 - 标题类型: {type(title).__name__} (값: {repr(title)[:50]}), 作者类型: {type(author).__name__} (값: {repr(author)[:50]})")
            return False
            
        if not isinstance(paragraphs, list):
            data_quality_stats['invalid_data_types'] += 1
            logger.warning(f"❌ 段落数据错误 - 标题: '{title}', 段落类型: {type(paragraphs).__name__}, 段落값: {repr(paragraphs)[:100]}")
            return False
        
        # 如果段落为空，标记为失传
        is_lost = len(paragraphs) == 0
        if is_lost:
            data_quality_stats['empty_paragraphs'] += 1
            paragraphs = ["失传，待考证"]
        
        # 繁简转换
        title_simplified = convert_to_simplified(title)
        author_simplified = convert_to_simplified(author)
        dynasty_simplified = convert_to_simplified(dynasty) if dynasty else ''
        
        # 检查是否已存在（基于标题+作者）
        check_sql = "SELECT COUNT(*) FROM t_poetry_master WHERE title = %s AND author = %s"
        cursor.execute(check_sql, (title_simplified, author_simplified))
        exists_count = cursor.fetchone()[0]
        
        if exists_count > 0:
            data_quality_stats['duplicate_poems'] += 1
            return True  # 静默跳过已存在的诗词，不记录任何日志
        
        # 验证内容质量（仅统计）
        full_content_traditional = ''.join(str(p) for p in paragraphs)
        validate_poetry_content(full_content_traditional, '完整内容', title)
        
        # 构建完整内容
        # paragraphs 中每个元素已包含完整标点，直接拼接即可
        full_content_simplified = convert_to_simplified(full_content_traditional)
        
        poetry_type = determine_poetry_type(dynasty_simplified)
        line_count = len(paragraphs)
        total_chars = calculate_total_chars(paragraphs)
        
        # 构建诗词结构JSON
        poem_structure = build_poem_structure(paragraphs)
        poem_structure_json = json.dumps(poem_structure, ensure_ascii=False)
        
        # 插入 t_poetry_master 表（新版表结构）
        master_sql = """
        INSERT INTO t_poetry_master 
        (title, author, dynasty, poetry_type, full_content_traditional, 
         full_content_simplified, poem_structure, line_count, total_chars, 
         is_verified, usage_count)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        
        cursor.execute(master_sql, (
            title_simplified,
            author_simplified,
            dynasty_simplified,
            poetry_type,
            full_content_traditional,
            full_content_simplified,
            poem_structure_json,
            line_count,
            total_chars,
            1,  # is_verified
            0   # usage_count
        ))
        
        master_id = cursor.lastrowid
        
        # 拆分半句并插入 t_poetry 表（新版表结构）
        all_lines = split_poetry_lines(paragraphs)
        poetry_lines = []
        
        for line in all_lines:
            if not line:
                continue
            
            # 验证半句内容（仅统计）
            validate_poetry_content(line, '半句', title)
            
            # 对半句进行繁简转换
            line_simplified = convert_to_simplified(line)
            
            poetry_lines.append((
                line_simplified,  # ✅ 使用转换后的简体内容
                author_simplified,
                title_simplified,
                dynasty_simplified,
                poetry_type,
                len(line_simplified),
                1,  # is_verified
                0,  # usage_count
                master_id
            ))
        
        if poetry_lines:
            line_sql = """
            INSERT INTO t_poetry 
            (content, author, title, dynasty, poetry_type, content_length,
             is_verified, usage_count, master_id)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            cursor.executemany(line_sql, poetry_lines)
        
        return True
        
    except pymysql.err.IntegrityError as e:
        logger.error(f"❌ 数据完整性错误（可能重复）- 标题: '{poem_data.get('title', '未知')}' - 错误: {e}")
        return False
    except pymysql.err.DataError as e:
        logger.error(f"❌ 数据错误 - 标题: '{poem_data.get('title', '未知')}' - 错误: {e}")
        return False
    except Exception as e:
        logger.error(f"❌ 插入失败 - 标题: '{poem_data.get('title', '未知')}' - 错误: {e}\n{traceback.format_exc()}")
        return False


def process_json_file(filepath, cursor, conn):
    """处理单个 JSON 文件"""
    try:
        # 验证文件路径
        if not os.path.exists(filepath):
            logger.error(f"文件不存在: {filepath}")
            return 0, 0
            
        if not os.path.isfile(filepath):
            logger.error(f"不是有效文件: {filepath}")
            return 0, 0
        
        # 检测文件大小
        file_size = os.path.getsize(filepath)
        if file_size > 100 * 1024 * 1024:  # 大于100MB
            logger.warning(f"⚠️ 文件较大: {os.path.basename(filepath)} ({file_size / (1024*1024):.2f} MB)")
        
        # 根据文件路径推断朝代
        dynasty_from_path = None
        if 'tang' in filepath.lower():
            dynasty_from_path = '唐'
        elif 'song' in filepath.lower():
            dynasty_from_path = '宋'
        
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        count = 0
        success = 0
        
        if isinstance(data, list):
            for item in data:
                if not isinstance(item, dict):
                    continue
                    
                # 全唐诗格式：嵌套结构
                if 'poems' in item:
                    author_name = item.get('name', '')
                    poems = item.get('poems', [])
                    if not isinstance(poems, list):
                        continue
                    for poem in poems:
                        if isinstance(poem, dict):
                            poem['author'] = author_name
                            # 如果诗词数据中没有朝代，使用从路径推断的朝代
                            if not poem.get('dynasty') and dynasty_from_path:
                                poem['dynasty'] = dynasty_from_path
                            if insert_poetry_data(cursor, poem):
                                success += 1
                            count += 1
                # 全宋词格式：扁平结构
                elif 'title' in item or 'rhythmic' in item:
                    if 'rhythmic' in item and 'title' not in item:
                        item['title'] = item['rhythmic']
                    # 如果诗词数据中没有朝代，使用从路径推断的朝代
                    if not item.get('dynasty') and dynasty_from_path:
                        item['dynasty'] = dynasty_from_path
                    if insert_poetry_data(cursor, item):
                        success += 1
                    count += 1
        else:
            logger.warning(f"文件格式不正确，期望列表类型: {filepath}")
        
        conn.commit()
        logger.info(f"文件处理完成: {os.path.basename(filepath)}, 总计: {count}, 成功: {success}")
        return count, success
        
    except json.JSONDecodeError as e:
        logger.error(f"JSON 解析失败 {filepath}: {e}")
        conn.rollback()
        return 0, 0
    except UnicodeDecodeError as e:
        logger.error(f"文件编码错误 {filepath}: {e}")
        conn.rollback()
        return 0, 0
    except Exception as e:
        logger.error(f"处理文件失败 {filepath}: {e}\n{traceback.format_exc()}")
        conn.rollback()
        return 0, 0


def import_directory(directory_path, description="数据"):
    """批量导入目录下的所有 JSON 文件"""
    # 验证目录路径
    if not directory_path or not isinstance(directory_path, str):
        print(f"❌ 无效的目录路径")
        logger.error("无效的目录路径")
        return
    
    if not os.path.exists(directory_path):
        print(f"❌ 目录不存在: {directory_path}")
        logger.error(f"目录不存在: {directory_path}")
        return
    
    if not os.path.isdir(directory_path):
        print(f"❌ 路径不是目录: {directory_path}")
        logger.error(f"路径不是目录: {directory_path}")
        return
    
    logger.info(f"\n{'='*60}")
    logger.info(f"开始导入{description}: {directory_path}")
    logger.info(f"{'='*60}\n")
    
    # 连接数据库
    conn = None
    cursor = None
    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()
        
        # 获取所有 JSON 文件
        json_files = [f for f in os.listdir(directory_path) if f.endswith('.json')]
        json_files.sort()
        
        if not json_files:
            print(f"⚠️ 目录中没有找到 JSON 文件: {directory_path}")
            logger.warning(f"目录中没有找到 JSON 文件: {directory_path}")
            return
        
        total_files = len(json_files)
        total_count = 0
        total_success = 0
        start_time = time.time()
        
        for idx, filename in enumerate(json_files, 1):
            filepath = os.path.join(directory_path, filename)
            
            file_count, file_success = process_json_file(filepath, cursor, conn)
            total_count += file_count
            total_success += file_success
            
            # 每处理10个文件或最后一个文件时显示进度
            if idx % 10 == 0 or idx == total_files:
                elapsed = time.time() - start_time
                speed = total_count / elapsed if elapsed > 0 else 0
                eta = (total_files - idx) / (idx / elapsed) if idx > 0 and elapsed > 0 else 0
                
                print(f"[{idx}/{total_files}] 已处理: {total_count} 首 | "
                      f"成功: {total_success} | "
                      f"速度: {speed:.1f} 首/秒 | "
                      f"预计剩余: {eta/60:.1f} 分钟")
        
        total_time = time.time() - start_time
        print(f"\n{'='*60}")
        print(f"✅ {description}导入完成！")
        print(f"   总文件数: {total_files}")
        print(f"   总诗词数: {total_count}")
        print(f"   成功导入: {total_success}")
        print(f"   失败数量: {total_count - total_success}")
        print(f"   耗时: {total_time/60:.2f} 分钟")
        print(f"   平均速度: {total_count/total_time if total_time > 0 else 0:.1f} 首/秒")
        
        # 显示繁简转换统计信息
        if conversion_stats['total_conversions'] > 0:
            print(f"\n📊 繁简转换统计:")
            print(f"   总转换次数: {conversion_stats['total_conversions']}")
            print(f"   长度变化次数: {conversion_stats['length_changed']}")
            print(f"   转换失败次数: {conversion_stats['failed_conversions']}")
            
            if conversion_stats['lost_characters']:
                print(f"\n⚠️  发现字符丢失（可能是生僻字）:")
                # 按出现频率排序，显示前20个最常见的丢失字符
                most_common_lost = conversion_stats['lost_characters'].most_common(20)
                for char, count in most_common_lost:
                    print(f"   '{char}' (Unicode: U+{ord(char):04X}) - 出现 {count} 次")
                
                if len(conversion_stats['lost_characters']) > 20:
                    print(f"   ... 还有 {len(conversion_stats['lost_characters']) - 20} 种字符")
                
                logger.warning(f"繁简转换字符丢失统计 - 总丢失字符种类: {len(conversion_stats['lost_characters'])}, 详情见上方输出")
            else:
                print(f"   ✅ 未发现字符丢失")
        
        # 显示数据质量统计
        print(f"\n🔍 数据质量报告:")
        total_issues = sum([
            data_quality_stats['empty_titles'],
            data_quality_stats['empty_authors'],
            data_quality_stats['empty_paragraphs'],
            data_quality_stats['invalid_data_types'],
            data_quality_stats['unusual_punctuation'],
            data_quality_stats['very_long_lines'],
            data_quality_stats['very_short_lines'],
            data_quality_stats['missing_dynasty']
        ])
        
        if total_issues == 0 and not data_quality_stats['special_characters']:
            print(f"   ✅ 数据质量良好，未发现明显问题")
        else:
            if data_quality_stats['empty_titles'] > 0:
                print(f"   ⚠️  空标题: {data_quality_stats['empty_titles']} 条")
            if data_quality_stats['empty_authors'] > 0:
                print(f"   ⚠️  空作者: {data_quality_stats['empty_authors']} 条")
            if data_quality_stats['empty_paragraphs'] > 0:
                print(f"   ℹ️  失传诗词: {data_quality_stats['empty_paragraphs']} 条")
            if data_quality_stats['missing_dynasty'] > 0:
                print(f"   ⚠️  缺失朝代: {data_quality_stats['missing_dynasty']} 条")
            if data_quality_stats['invalid_data_types'] > 0:
                print(f"   ❌ 数据类型错误: {data_quality_stats['invalid_data_types']} 条")
            if data_quality_stats['duplicate_poems'] > 0:
                print(f"   ℹ️  重复诗词（已跳过）: {data_quality_stats['duplicate_poems']} 条")
            if data_quality_stats['unusual_punctuation'] > 0:
                print(f"   ⚠️  异常标点: {data_quality_stats['unusual_punctuation']} 处")
            if data_quality_stats['very_long_lines'] > 0:
                print(f"   ⚠️  超长句子: {data_quality_stats['very_long_lines']} 个")
            if data_quality_stats['very_short_lines'] > 0:
                print(f"   ⚠️  超短句子: {data_quality_stats['very_short_lines']} 个")
            
            if data_quality_stats['special_characters']:
                print(f"\n   🔤 特殊字符统计（前20种）:")
                most_common_special = data_quality_stats['special_characters'].most_common(20)
                for char, count in most_common_special:
                    char_name = f"U+{ord(char):04X}"
                    try:
                        import unicodedata
                        char_desc = unicodedata.name(char, '未知字符')
                    except:
                        char_desc = '未知字符'
                    print(f"      '{char}' ({char_name}, {char_desc}) - 出现 {count} 次")
                
                if len(data_quality_stats['special_characters']) > 20:
                    print(f"      ... 还有 {len(data_quality_stats['special_characters']) - 20} 种字符")
                
                logger.warning(f"特殊字符统计 - 总种类: {len(data_quality_stats['special_characters'])}, 详情见上方输出")
        
        print(f"{'='*60}\n")
        
        logger.info(f"{description}导入完成 - 文件: {total_files}, 总计: {total_count}, 成功: {total_success}, 耗时: {total_time/60:.2f} 분钟")
        logger.info(f"数据质量统计 - 总问题数: {total_issues}")
        
    except pymysql.err.OperationalError as e:
        print(f"❌ 数据库操作错误: {e}")
        logger.error(f"数据库操作错误: {e}")
    except pymysql.err.ProgrammingError as e:
        print(f"❌ SQL 语法错误: {e}")
        logger.error(f"SQL 语法错误: {e}")
    except Exception as e:
        print(f"❌ 导入过程发生错误: {e}")
        logger.error(f"导入过程发生错误: {e}\n{traceback.format_exc()}")
    finally:
        # 确保资源正确释放
        if cursor:
            try:
                cursor.close()
            except:
                pass
        if conn:
            try:
                conn.close()
            except:
                pass


def get_user_choice():
    """获取用户选择，带输入验证"""
    while True:
        try:
            choice = input("\n请输入选项 (1-4): ").strip()
            if choice in ['1', '2', '3', '4']:
                return choice
            else:
                print("❌ 无效输入，请输入 1-4 之间的数字")
        except EOFError:
            print("\n⚠️ 检测到输入结束")
            sys.exit(0)
        except KeyboardInterrupt:
            print("\n\n⚠️ 用户取消操作")
            sys.exit(0)


def main():
    """主函数"""
    print("="*60)
    print("   诗词数据批量导入工具 v2.0")
    print("   （适配新版对战逻辑与数据库结构）")
    print("="*60)
    print()
    
    # 检查数据库连接
    try:
        conn = pymysql.connect(**DB_CONFIG)
        conn.close()
        print("✅ 数据库连接成功")
    except pymysql.err.OperationalError as e:
        print(f"❌ 数据库连接失败: {e}")
        logger.error(f"❌ 数据库连接失败: {e}")
        print("   请检查:")
        print("   1. MySQL 服务是否启动")
        print("   2. DB_CONFIG 配置是否正确（主机、端口、用户名、密码）")
        print("   3. 数据库 'feihualingbattle' 是否存在")
        sys.exit(1)
    except Exception as e:
        print(f"❌ 数据库连接异常: {e}")
        logger.error(f"❌ 数据库连接异常: {e}")
        sys.exit(1)
    
    # 验证数据目录
    print("\n📂 检查数据目录...")
    
    tang_exists = os.path.exists(TANG_DIR)
    song_exists = os.path.exists(SONG_DIR)
    
    if not tang_exists:
        print(f"❌ 全唐诗目录不存在: {TANG_DIR}")
        logger.warning(f"⚠️ 全唐诗目录不存在: {TANG_DIR}")
    else:
        tang_files = [f for f in os.listdir(TANG_DIR) if f.endswith('.json')]
        print(f"✅ 全唐诗目录存在: {len(tang_files)} 个 JSON 文件")
    
    if not song_exists:
        print(f"❌ 全宋词目录不存在: {SONG_DIR}")
        logger.warning(f"⚠️ 全宋词目录不存在: {SONG_DIR}")
    else:
        song_files = [f for f in os.listdir(SONG_DIR) if f.endswith('.json')]
        print(f"✅ 全宋词目录存在: {len(song_files)} 个 JSON 文件")
    
    print()
    
    # 如果两个目录都不存在，给出明确提示
    if not tang_exists and not song_exists:
        print("⚠️  警告: 数据目录不存在！")
        logger.warning("⚠️ 数据目录不存在")
        print("\n可能的原因:")
        print("  1. chinese-poetry-data 文件夹未下载或未放置到正确位置")
        print("  2. 目录名称拼写错误")
        print("  3. 使用了不同的项目路径")
        print("\n解决方法:")
        print(f"  - 确保以下目录存在:")
        print(f"    {os.path.join(BASE_DIR, 'chinese-poetry-data')}")
        print(f"  - 或者使用选项 4 指定自定义目录路径")
        print()
        
        # 询问用户是否继续
        continue_anyway = input("是否继续使用自定义目录？(y/n): ").strip().lower()
        if continue_anyway != 'y':
            print("\n程序退出。请先准备好数据目录后再运行。")
            sys.exit(0)
    
    # 选择导入模式
    print("请选择导入模式:")
    print("1. 仅导入全唐诗")
    print("2. 仅导入全宋词")
    print("3. 导入全部数据")
    print("4. 自定义目录")
    
    choice = get_user_choice()
    
    try:
        if choice == '1':
            if not tang_exists:
                print(f"❌ 全唐诗目录不存在，无法导入")
                logger.error(f"❌ 全唐诗目录不存在，无法导入: {TANG_DIR}")
                sys.exit(1)
            import_directory(TANG_DIR, "全唐诗")
        elif choice == '2':
            if not song_exists:
                print(f"❌ 全宋词目录不存在，无法导入")
                logger.error(f"❌ 全宋词目录不存在，无法导入: {SONG_DIR}")
                sys.exit(1)
            import_directory(SONG_DIR, "全宋词")
        elif choice == '3':
            if not tang_exists and not song_exists:
                print(f"❌ 数据目录不存在，无法导入")
                logger.error("❌ 数据目录不存在，无法导入")
                sys.exit(1)
            if tang_exists:
                import_directory(TANG_DIR, "全唐诗")
            if song_exists:
                import_directory(SONG_DIR, "全宋词")
        elif choice == '4':
            custom_dir = input("请输入目录路径: ").strip()
            if not custom_dir:
                print("❌ 目录路径不能为空")
                logger.error("❌ 自定义目录路径为空")
                sys.exit(1)
            # 处理相对路径和绝对路径
            if not os.path.isabs(custom_dir):
                custom_dir = os.path.join(os.getcwd(), custom_dir)
            if not os.path.exists(custom_dir):
                print(f"❌ 目录不存在: {custom_dir}")
                logger.error(f"❌ 自定义目录不存在: {custom_dir}")
                sys.exit(1)
            import_directory(custom_dir, "自定义数据")
    except KeyboardInterrupt:
        print("\n\n⚠️ 用户中断导入")
        logger.warning("⚠️ 用户中断导入")
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ 导入过程发生错误: {e}")
        logger.error(f"❌ 导入过程发生错误: {e}\n{traceback.format_exc()}")
        sys.exit(1)
    
    print("\n🎉 所有操作完成！")
    print("\n验证数据:")
    print("  SELECT COUNT(*) FROM t_poetry_master;")
    print("  SELECT COUNT(*) FROM t_poetry;")


if __name__ == '__main__':
    main()
